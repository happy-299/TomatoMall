<script setup lang="ts">
import { onMounted, ref, nextTick, onBeforeUnmount, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { getProducts, type Product, getStockpile } from '../../api/product';
import ProductCard from '../../components/ProductCard.vue';
import BookListItem from '../../components/BookListItem.vue';
import ReadingNote from '../../components/ReadingNote.vue';
import {
  getTopBookLists,
  type BookListVO,
  collectBookList,
  cancelCollectBookList,
  deleteBookList,
  getFavouriteBookLists,
  addItemToBookList,
  removeItemFromBookList
} from '../../api/booklist';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { getUserInfo } from '../../api/user';
import { 
  getTopLikedNotes, 
  type NoteVO, 
  getNoteLikeStatus, 
  getNotePayStatus, 
  likeNote, 
  unlikeNote, 
  payNote, 
  viewNote 
} from '../../api/note';

const router = useRouter();
const products = ref<Product[]>([]);
const stockpiles = ref<Record<string, { amount: number, frozen: number }>>({});
const loading = ref(true);
const cartItems = ref<Record<string, { cartItemId: string; quantity: number }>>({});
const bookLists = ref<BookListVO[]>([]);
const loadingBookLists = ref(false);

// 读书笔记相关状态
const topNotes = ref<NoteVO[]>([]);
const loadingNotes = ref(false);
const likedNoteIds = ref<Set<number>>(new Set());
const paidNoteIds = ref<Set<number>>(new Set());
const detailNoteDialogVisible = ref(false);
const currentNote = ref<NoteVO | null>(null);
const showPurchaseDialog = ref(false);
const selectedNote = ref<NoteVO | null>(null);
const confirmLoading = ref(false);

// Navigate to product list page
const goToProductList = () => {
  router.push({ path: "/productList" });
};

// Navigate to booklist page
const goToBookList = () => {
  router.push({ path: "/booklist" });
};

// Fetch products for the featured section
const fetchProducts = async () => {
  try {
    loading.value = true;
    const response = await getProducts();
    if (response.data.code === '200') {
      // Get only the first 4 products for the featured section
      products.value = response.data.data.slice(0, 4);

      // Fetch stockpile information for each product
      await Promise.all(
          products.value.map(async (product) => {
            try {
              const stockRes = await getStockpile(product.id);
              if (stockRes.data.code === '200') {
                stockpiles.value[product.id] = stockRes.data.data;
              }
            } catch (error) {
              console.error(`Failed to fetch stockpile for product ${product.id}`, error);
            }
          })
      );
    }
  } catch (error) {
    console.error('Failed to fetch products', error);
  } finally {
    loading.value = false;
  }
};

// Fetch book lists
const fetchBookLists = async () => {
  try {
    loadingBookLists.value = true;
    console.log('Fetching top book lists...');
    const response: any = await getTopBookLists();
    console.log('Top BookLists API response:', response);

    // 检查后端返回的数据结构
    if (response && response.data && response.data.code === '200' && response.data.data) {
      // 使用后端返回的热门书单数据
      const rawBookLists = response.data.data;
      console.log('Raw top book lists from backend:', rawBookLists);

      bookLists.value = rawBookLists.map(ensureValidBookList);
      console.log('Processed book lists:', bookLists.value);

      // 获取收藏状态
      await fetchFavouriteBookListIds();
    } else {
      console.error('Unexpected API response structure:', response);
      // 只有在API返回结构不符合预期时才使用测试数据
      useTestData();
    }

    // 如果没有获取到数据，使用测试数据
    if (bookLists.value.length === 0) {
      console.log('No book lists found, using test data');
      useTestData();
    }
  } catch (error) {
    console.error('Failed to fetch book lists', error);
    // 使用测试数据
    useTestData();
  } finally {
    loadingBookLists.value = false;
  }
};

// Handle book list actions
const handleBookListView = (bookList: BookListVO) => {
  // 显示书单详情对话框
  currentBookList.value = bookList;
  detailDialogVisible.value = true;
  console.log('查看书单详情:', bookList.title);
};

const handleBookListCollect = async (bookList: BookListVO) => {
  try {
    const isCollected = favouriteBookListIds.value.has(bookList.id);
    if (isCollected) {
      await cancelCollectBookList({bookListId: bookList.id});
      bookList.favouriteCount--;
      favouriteBookListIds.value.delete(bookList.id);
      ElMessage.success('取消收藏成功');
    } else {
      await collectBookList({bookListId: bookList.id});
      bookList.favouriteCount++;
      favouriteBookListIds.value.add(bookList.id);
      ElMessage.success('收藏成功');
    }
  } catch (error) {
    console.error('收藏操作失败:', error);
    ElMessage.error('操作失败');
  }
};

const handleBookListDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
        '确定要删除这个书单吗？',
        '删除确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );
    await deleteBookList(id);
    ElMessage.success('删除书单成功');
    await fetchBookLists(); // 重新获取书单列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除书单失败:', error);
      ElMessage.error('删除书单失败');
    }
  }
};

// 获取收藏的书单ID列表
const fetchFavouriteBookListIds = async () => {
  try {
    const res: any = await getFavouriteBookLists(0, 1000);
    if (res.data && res.data.data) {
      favouriteBookListIds.value = new Set(res.data.data.content.map((book: BookListVO) => book.id));
    }
  } catch (error) {
    console.error('获取收藏书单失败:', error);
  }
};

// 书单相关状态
const currentUserId = ref<number | null>(null);
const favouriteBookListIds = ref<Set<number>>(new Set());
const detailDialogVisible = ref(false);
const currentBookList = ref<BookListVO | null>(null);
const selectedProduct = ref<string | null>(null);

// 从书单移除商品
const handleRemoveProduct = async (bookListId: number, productId: string) => {
  try {
    await removeItemFromBookList(bookListId, Number(productId));

    // 更新当前书单的商品列表
    if (currentBookList.value) {
      currentBookList.value.products = currentBookList.value.products.filter(p => p.id !== productId);
    }

    ElMessage.success('移除商品成功');
  } catch (error) {
    console.error('移除商品失败:', error);
    ElMessage.error('移除商品失败');
  }
};

// 添加商品到书单
const handleAddProduct = async (bookListId: number) => {
  if (!selectedProduct.value) {
    ElMessage.warning('请先选择要添加的商品');
    return;
  }

  try {
    await addItemToBookList(bookListId, Number(selectedProduct.value));

    // 更新当前书单的商品列表
    if (currentBookList.value) {
      const addedProduct = products.value.find(p => p.id === selectedProduct.value);
      if (addedProduct) {
        currentBookList.value.products = [...currentBookList.value.products, addedProduct];
      }
    }

    ElMessage.success('添加商品成功');
    selectedProduct.value = null; // 清空选择
  } catch (error) {
    console.error('添加商品失败:', error);
    ElMessage.error('添加商品失败');
  }
};

// 处理商品点击
const handleProductClick = (productId: string) => {
  router.push(`/product/${productId}`);
};

// 确保书单数据符合组件要求
const ensureValidBookList = (bookList: any): BookListVO => {
  console.log('Processing book list:', bookList);

  // 确保products是一个数组
  if (!bookList.products || !Array.isArray(bookList.products)) {
    console.log('Products is not an array or null, setting to empty array');
    bookList.products = [];
  }

  // 确保每个产品都有cover属性
  bookList.products = bookList.products.map((product: any) => {
    // 如果product是null或undefined，提供一个默认对象
    if (!product) {
      console.log('Product is null or undefined, using default product');
      return {
        id: '0',
        title: '未知书籍',
        price: 0,
        rate: 0,
        description: '',
        cover: 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png',
        detail: '',
        specifications: []
      };
    }

    return {
      ...product,
      cover: product.cover || 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png' // 默认图片
    };
  });

  // 确保其他必要属性存在
  const result = {
    id: bookList.id || 0,
    title: bookList.title || '未命名书单',
    creatorId: bookList.creatorId || 0,
    description: bookList.description || '暂无描述',
    picture: bookList.picture || '',
    products: bookList.products,
    creatorName: bookList.creatorName || '匿名用户',
    creatorAvatar: bookList.creatorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    creationDate: bookList.creationDate || '',
    favouriteCount: bookList.favouriteCount || 0
  };

  console.log('Processed book list result:', result);
  return result;
};

// 使用测试数据
const useTestData = () => {
  bookLists.value = [
    {
      id: 1,
      title: '经典文学名著',
      creatorId: 1,
      description: '包含世界经典文学名著，适合文学爱好者阅读',
      picture: '',
      products: [
        {
          id: '1',
          title: '红楼梦',
          price: 59.9,
          rate: 4.9,
          description: '中国古典四大名著之一',
          cover: 'https://img9.doubanio.com/view/subject/s/public/s1070959.jpg',
          detail: '《红楼梦》是中国古典四大名著之一，通过描写贾、史、王、薛四大家族的兴衰，展示了封建社会末期的社会生活。',
          specifications: []
        },
        {
          id: '5',
          title: '西游记',
          price: 49.9,
          rate: 4.8,
          description: '中国古典四大名著之一',
          cover: 'https://img2.doubanio.com/view/subject/s/public/s1627374.jpg',
          detail: '《西游记》是中国古典四大名著之一，主要描写了孙悟空保护唐僧西天取经，沿途降妖除魔的故事。',
          specifications: []
        }
      ],
      creatorName: '文学爱好者',
      creatorAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      creationDate: '2023-05-01',
      favouriteCount: 128
    },
    {
      id: 2,
      title: '科技前沿书籍',
      creatorId: 2,
      description: '关于人工智能、区块链等前沿科技的书籍推荐',
      picture: '',
      products: [
        {
          id: '2',
          title: '人工智能简史',
          price: 79.9,
          rate: 4.7,
          description: '人工智能发展历程',
          cover: 'https://img2.doubanio.com/view/subject/s/public/s29735609.jpg',
          detail: '本书介绍了人工智能的发展历程、关键技术及未来展望。',
          specifications: []
        },
        {
          id: '6',
          title: '区块链革命',
          price: 69.9,
          rate: 4.5,
          description: '区块链技术解析',
          cover: 'https://img1.doubanio.com/view/subject/s/public/s29188373.jpg',
          detail: '本书详细介绍了区块链技术的原理、应用场景及未来发展趋势。',
          specifications: []
        }
      ],
      creatorName: '科技迷',
      creatorAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      creationDate: '2023-05-15',
      favouriteCount: 96
    },
    {
      id: 3,
      title: '心理学入门书单',
      creatorId: 3,
      description: '适合心理学初学者的入门书籍推荐',
      picture: '',
      products: [
        {
          id: '3',
          title: '心理学与生活',
          price: 68.0,
          rate: 4.8,
          description: '心理学经典教材',
          cover: 'https://img1.doubanio.com/view/subject/s/public/s1065691.jpg',
          detail: '《心理学与生活》是心理学专业最经典的教材之一，内容全面且通俗易懂。',
          specifications: []
        },
        {
          id: '7',
          title: '乌合之众',
          price: 38.0,
          rate: 4.6,
          description: '群体心理学经典著作',
          cover: 'https://img9.doubanio.com/view/subject/s/public/s1988393.jpg',
          detail: '《乌合之众》是法国社会心理学家古斯塔夫·勒庞的著作，探讨了群体心理的特点和表现。',
          specifications: []
        }
      ],
      creatorName: '心理学爱好者',
      creatorAvatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
      creationDate: '2023-06-01',
      favouriteCount: 85
    },
    {
      id: 4,
      title: '儿童读物精选',
      creatorId: 4,
      description: '适合3-6岁儿童阅读的优质绘本',
      picture: '',
      products: [
        {
          id: '4',
          title: '好饿的毛毛虫',
          price: 39.8,
          rate: 5.0,
          description: '经典儿童绘本',
          cover: 'https://img9.doubanio.com/view/subject/s/public/s1597864.jpg',
          detail: '这是一本色彩鲜艳、故事生动的儿童绘本，深受孩子们的喜爱。',
          specifications: []
        },
        {
          id: '8',
          title: '猜猜我有多爱你',
          price: 35.0,
          rate: 4.9,
          description: '温馨亲子绘本',
          cover: 'https://img1.doubanio.com/view/subject/s/public/s1067911.jpg',
          detail: '这是一本表达亲子之爱的绘本，温馨感人，适合亲子共读。',
          specifications: []
        }
      ],
      creatorName: '绘本妈妈',
      creatorAvatar: 'https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg',
      creationDate: '2023-06-15',
      favouriteCount: 156
    }
  ];
  console.log('Using test data for booklists:', bookLists.value);

  // 确保所有书单数据都符合组件要求
  bookLists.value = bookLists.value.map(ensureValidBookList);
};

// Confetti animation variables
let canvas: HTMLCanvasElement | null = null;
let ctx: CanvasRenderingContext2D | null = null;
let confetti: any[] = [];
let sequins: any[] = [];
let animationFrameId: number | null = null;

// Colors for confetti
const colors = [
  { front: '#FF6347', back: '#D9534F' }, // Tomato red
  { front: '#FF8C69', back: '#E57373' }, // Salmon
  { front: '#FFA07A', back: '#EF9A9A' }, // Light salmon
  { front: '#FFD700', back: '#FFC107' }, // Gold/Amber
  { front: '#28a745', back: '#20c997' }, // Green/Teal
  { front: '#f8f9fa', back: '#e9ecef' }  // Light gray/white
];

// Physics constants
const gravity = 0.2;
const drag = 0.05;
const terminalVelocity = 5;

// Helper functions
const randomRange = (min: number, max: number) => Math.random() * (max - min) + min;

// Confetto Class
function Confetto(this: any) {
  this.randomModifier = randomRange(0, 99);
  this.color = colors[Math.floor(randomRange(0, colors.length))];
  this.dimensions = {
    x: randomRange(5, 10),
    y: randomRange(8, 16),
  };

  // Start from bottom center of screen
  const centerX = window.innerWidth / 2;
  const spreadX = randomRange(-15, 15);

  this.position = {
    x: centerX + spreadX,
    y: window.innerHeight - 100, // A bit above the bottom
  };

  this.rotation = randomRange(0, 2 * Math.PI);
  this.scale = {
    x: 1,
    y: 1,
  };

  // Initial velocity - shoot upward with random angle
  const angle = randomRange(-Math.PI / 3, Math.PI / 3); // -60° to 60°
  const speed = randomRange(15, 30);

  this.velocity = {
    x: Math.sin(angle) * speed,
    y: Math.cos(angle) * -speed, // Negative for upward movement
  };
}

Confetto.prototype.update = function() {
  // Apply forces to velocity
  this.velocity.x *= (1 - drag);
  this.velocity.y += gravity;
  this.velocity.y = Math.min(this.velocity.y, terminalVelocity);

  // Add some wiggle
  this.velocity.x += randomRange(-0.3, 0.3);

  // Set position
  this.position.x += this.velocity.x;
  this.position.y += this.velocity.y;

  // Spin confetto by scaling y and rotation
  this.rotation += randomRange(0.01, 0.05);
  this.scale.y = Math.cos((this.position.y + this.randomModifier) * 0.09);
};

// Sequin Class
function Sequin(this: any) {
  this.color = colors[Math.floor(randomRange(0, colors.length))].back;
  this.radius = randomRange(1, 3);

  // Start from bottom center of screen
  const centerX = window.innerWidth / 2;
  const spreadX = randomRange(-15, 15);

  this.position = {
    x: centerX + spreadX,
    y: window.innerHeight - 100, // A bit above the bottom
  };

  // Initial velocity - shoot upward with random angle
  const angle = randomRange(-Math.PI / 3, Math.PI / 3); // -60° to 60°
  const speed = randomRange(15, 25);

  this.velocity = {
    x: Math.sin(angle) * speed,
    y: Math.cos(angle) * -speed, // Negative for upward movement
  };
}

Sequin.prototype.update = function() {
  // Apply forces to velocity
  this.velocity.x *= (1 - drag * 0.5);
  this.velocity.y += gravity * 0.8; // Sequins are lighter

  // Add some wiggle
  this.velocity.x += randomRange(-0.2, 0.2);

  // Set position
  this.position.x += this.velocity.x;
  this.position.y += this.velocity.y;
};

// Add elements to arrays to be drawn - create a larger initial burst
const initBurst = () => {
  // Add new confetti - more for the initial burst
  for (let i = 0; i < 100; i++) {
    confetti.push(new (Confetto as any)());
  }

  // Add new sequins - more for the initial burst
  for (let i = 0; i < 50; i++) {
    sequins.push(new (Sequin as any)());
  }
};

// Draw the elements on the canvas
const render = () => {
  if (!ctx || !canvas) return;

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  confetti.forEach((confetto) => {
    let width = (confetto.dimensions.x * confetto.scale.x);
    let height = (confetto.dimensions.y * confetto.scale.y);

    // Move canvas to position and rotate
    ctx!.translate(confetto.position.x, confetto.position.y);
    ctx!.rotate(confetto.rotation);

    // Update confetto "physics" values
    confetto.update();

    // Get front or back fill color
    ctx!.fillStyle = confetto.scale.y > 0 ? confetto.color.front : confetto.color.back;

    // Draw confetto
    ctx!.fillRect(-width / 2, -height / 2, width, height);

    // Reset transform matrix
    ctx!.setTransform(1, 0, 0, 1, 0, 0);
  });

  sequins.forEach((sequin) => {
    // Move canvas to position
    ctx!.translate(sequin.position.x, sequin.position.y);

    // Update sequin "physics" values
    sequin.update();

    // Set the color
    ctx!.fillStyle = sequin.color;

    // Draw sequin
    ctx!.beginPath();
    ctx!.arc(0, 0, sequin.radius, 0, 2 * Math.PI);
    ctx!.fill();

    // Reset transform matrix
    ctx!.setTransform(1, 0, 0, 1, 0, 0);
  });

  // Remove confetti and sequins that fall off the screen
  confetti = confetti.filter(confetto => {
    return confetto.position.y < canvas!.height + 100 &&
        confetto.position.y > -100 &&
        confetto.position.x > -100 &&
        confetto.position.x < canvas!.width + 100;
  });

  sequins = sequins.filter(sequin => {
    return sequin.position.y < canvas!.height + 100 &&
        sequin.position.y > -100 &&
        sequin.position.x > -100 &&
        sequin.position.x < canvas!.width + 100;
  });

  // Continue animation if there are still elements to animate
  if (confetti.length > 0 || sequins.length > 0) {
    animationFrameId = window.requestAnimationFrame(render);
  } else {
    // Clean up when all confetti are gone
    if (canvas && canvas.parentNode) {
      canvas.style.display = 'none';
    }
  }
};

// Resize canvas if the window size changes
const resizeCanvas = () => {
  if (canvas) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
};

// Initialize confetti animation
const initConfetti = () => {
  nextTick(() => {
    canvas = document.getElementById('confetti-canvas') as HTMLCanvasElement;
    if (canvas) {
      ctx = canvas.getContext('2d');

      // Set canvas size
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;

      // Add window resize listener
      window.addEventListener('resize', resizeCanvas);

      // Initialize and start animation - just one burst
      initBurst();
      animationFrameId = window.requestAnimationFrame(render);
    }
  });
};

// Clean up event listeners and animation frame
const cleanupConfetti = () => {
  window.removeEventListener('resize', resizeCanvas);
  if (animationFrameId !== null) {
    window.cancelAnimationFrame(animationFrameId);
  }
};

// 获取热门笔记
const fetchTopNotes = async () => {
  try {
    loadingNotes.value = true;
    console.log('Fetching top notes...');
    const response = await getTopLikedNotes(4);
    console.log('Top notes API response:', response);
    
    // 检查响应结构 - 根据实际API返回结构调整
    if (response && response.data && typeof response.data === 'object') {
      // 处理后端返回的嵌套数据结构
      // @ts-ignore - 忽略类型检查，因为我们已经进行了运行时检查
      if (response.data.code === '200' && Array.isArray(response.data.data)) {
        // @ts-ignore - 忽略类型检查
        topNotes.value = response.data.data;
        console.log('Notes data found:', topNotes.value);
      } else if (Array.isArray(response.data)) {
        // 直接是数组的情况
        topNotes.value = response.data;
        console.log('Notes data found in direct array:', topNotes.value);
      } else {
        console.log('Invalid data structure in response');
        useTestNotes();
        return;
      }
      
      // 检查点赞和购买状态
      if (topNotes.value.length > 0) {
        await checkNotesStatus();
      } else {
        // 如果没有数据，使用测试数据
        useTestNotes();
      }
    } else {
      console.log('Invalid response structure:', response);
      // 使用测试数据
      useTestNotes();
    }
  } catch (error) {
    console.error('获取热门笔记失败:', error);
    // 使用测试数据
    useTestNotes();
  } finally {
    loadingNotes.value = false;
  }
};

// 使用测试数据
const useTestNotes = () => {
  topNotes.value = [
    {
      id: 1,
      title: '《红楼梦》读书笔记',
      content: '《红楼梦》是中国古典四大名著之一，通过描写贾、史、王、薛四大家族的兴衰，展示了封建社会末期的社会生活。',
      img: 'https://img9.doubanio.com/view/subject/s/public/s1070959.jpg',
      price: 0,
      creatorId: 1,
      createTime: '2023-05-01',
      viewCnt: 128,
      likeCnt: 42
    },
    {
      id: 2,
      title: '《活着》读后感',
      content: '余华的《活着》讲述了农民福贵的人生故事，展现了普通人在大时代背景下的生存状态。',
      img: 'https://img2.doubanio.com/view/subject/s/public/s29053580.jpg',
      price: 5,
      creatorId: 2,
      createTime: '2023-06-15',
      viewCnt: 96,
      likeCnt: 38
    },
    {
      id: 3,
      title: '《百年孤独》阅读心得',
      content: '马尔克斯的《百年孤独》是拉丁美洲魔幻现实主义文学的代表作，讲述了布恩迪亚家族七代人的故事。',
      img: 'https://img1.doubanio.com/view/subject/s/public/s6384944.jpg',
      price: 10,
      creatorId: 3,
      createTime: '2023-07-20',
      viewCnt: 85,
      likeCnt: 36
    },
    {
      id: 4,
      title: '《三体》科幻小说解析',
      content: '刘慈欣的《三体》是中国科幻文学的里程碑，探讨了宇宙文明之间的接触与冲突。',
      img: 'https://img9.doubanio.com/view/subject/s/public/s2768378.jpg',
      price: 8,
      creatorId: 4,
      createTime: '2023-08-10',
      viewCnt: 156,
      likeCnt: 48
    }
  ];
  console.log('Using test notes data:', topNotes.value);
};

// 检查笔记的点赞和购买状态
const checkNotesStatus = async () => {
  // 确保topNotes.value是一个数组且不为空
  if (!Array.isArray(topNotes.value) || topNotes.value.length === 0) {
    console.log('No notes to check status for');
    return;
  }
  
  console.log('Checking status for notes:', topNotes.value);
  
  // 清空状态集合，以便重新填充
  likedNoteIds.value.clear();
  paidNoteIds.value.clear();
  
  for (const note of topNotes.value) {
    try {
      if (!note || typeof note.id !== 'number') {
        console.log('Invalid note or note.id:', note);
        continue;
      }
      
      // 检查点赞状态
      console.log('Checking like status for note ID:', note.id);
      const likeRes = await getNoteLikeStatus(note.id);
      console.log('Like status response:', likeRes);
      
      // 处理响应结构 - 后端返回的数据可能在不同层级
      if (likeRes) {
        // 检查是否是嵌套结构
        if (typeof likeRes.data === 'object' && likeRes.data !== null) {
          // @ts-ignore - 忽略类型检查
          if (likeRes.data.code === '200' && likeRes.data.data === true) {
            likedNoteIds.value.add(note.id);
            console.log('Note is liked (nested):', note.id);
          }
        } else if (likeRes.data === true) {
          // 直接返回布尔值的情况
          likedNoteIds.value.add(note.id);
          console.log('Note is liked (direct):', note.id);
        }
      }
      
      // 检查购买状态（如果笔记需要付费）
      if (note.price > 0) {
        console.log('Checking payment status for note ID:', note.id);
        const payRes = await getNotePayStatus(note.id);
        console.log('Payment status response:', payRes);
        
        // 处理响应结构 - 后端返回的数据可能在不同层级
        if (payRes) {
          // 检查是否是嵌套结构
          if (typeof payRes.data === 'object' && payRes.data !== null) {
            // @ts-ignore - 忽略类型检查
            if (payRes.data.code === '200' && payRes.data.data === true) {
              paidNoteIds.value.add(note.id);
              console.log('Note is paid (nested):', note.id);
            }
          } else if (payRes.data === true) {
            // 直接返回布尔值的情况
            paidNoteIds.value.add(note.id);
            console.log('Note is paid (direct):', note.id);
          }
        }
      }
    } catch (error) {
      console.error('检查笔记状态失败:', error);
    }
  }
  
  console.log('Final liked notes IDs:', [...likedNoteIds.value]);
  console.log('Final paid notes IDs:', [...paidNoteIds.value]);
};

// 处理笔记点赞
const handleNoteLike = async (note: NoteVO) => {
  try {
    await likeNote(note.id);
    note.likeCnt++;
    likedNoteIds.value.add(note.id);
    ElMessage.success('点赞成功');
  } catch (error) {
    console.error('点赞失败:', error);
    ElMessage.error('点赞失败');
  }
};

// 处理取消点赞
const handleNoteUnlike = async (note: NoteVO) => {
  try {
    await unlikeNote(note.id);
    note.likeCnt--;
    likedNoteIds.value.delete(note.id);
    ElMessage.success('取消点赞成功');
  } catch (error) {
    console.error('取消点赞失败:', error);
    ElMessage.error('取消点赞失败');
  }
};

// 处理笔记删除
const handleNoteDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个笔记吗？',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    // 这里只是前端删除，实际应该调用删除API
    topNotes.value = topNotes.value.filter(n => n.id !== id);
    ElMessage.success('删除笔记成功');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除笔记失败:', error);
      ElMessage.error('删除笔记失败');
    }
  }
};

// 处理笔记购买
const handleNotePurchase = (note: NoteVO) => {
  selectedNote.value = note;
  showPurchaseDialog.value = true;
};

// 确认购买
const confirmPurchase = async () => {
  if (!selectedNote.value) return;

  try {
    confirmLoading.value = true;
    const loading = ElLoading.service({
      lock: true,
      text: '正在购买...',
      background: 'rgba(0, 0, 0, 0.7)'
    });

    try {
      const res = await payNote(selectedNote.value.id);

      // 增加业务状态码检查
      if (res.data && res.data.code === '200') {
        ElMessage.success('购买成功');
        paidNoteIds.value.add(selectedNote.value.id);
      } else {
        throw new Error(res.data?.msg || '购买失败');
      }
      
      showPurchaseDialog.value = false;
    } finally {
      loading.close();
      confirmLoading.value = false;
    }
  } catch (error: any) {
    // 增强错误处理逻辑
    const errorMessage = error.response?.data?.msg ||
        error.message ||
        '购买失败，请检查网络连接或账户余额';

    // 特殊处理余额不足情况
    if (errorMessage.includes('余额不足')) {
      ElMessage.error({
        message: errorMessage,
        duration: 5000,
        showClose: true
      });
    } else {
      ElMessage.error(errorMessage);
    }
    confirmLoading.value = false;
  }
};

// 查看笔记详情
const handleViewNote = async (note: NoteVO) => {
  try {
    currentNote.value = note;
    // 增加浏览量
    await viewNote(note.id);
    note.viewCnt++;
    detailNoteDialogVisible.value = true;
  } catch (error) {
    console.error('查看笔记失败:', error);
    ElMessage.error('查看笔记失败');
  }
};

// 获取显示内容
const getDisplayContent = (content: string, isPaid: boolean) => {
  if (isPaid || !content) return content;
  const showLength = Math.ceil(content.length * 0.35);
  return content.slice(0, showLength) + '...';
};

// Initialize data and animations
onMounted(() => {
  fetchProducts();
  fetchBookLists();
  fetchTopNotes();
  initConfetti();

  // 获取用户信息
  getUserCurrentInfo();
});

// Clean up before component is unmounted
onBeforeUnmount(() => {
  cleanupConfetti();
});

// Handle cart operations
const handleCartAdd = (productId: string) => {
  // This would typically call your cart API
  console.log('Add to cart:', productId);
};

const handleCartSubtract = (productId: string) => {
  // This would typically call your cart API
  console.log('Remove from cart:', productId);
};

const handleCartUpdated = () => {
  // This would typically refresh your cart data
  console.log('Cart updated');
};

// 获取当前用户信息
const getUserCurrentInfo = async () => {
  try {
    const username = sessionStorage.getItem('username');
    if (username) {
      const res = await getUserInfo(username);
      if (res.data && res.data.data) {
        currentUserId.value = res.data.data.id;
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};
</script>

<template>
  <div class="page-container">
    <!-- Canvas for confetti animation -->
    <canvas id="confetti-canvas"></canvas>

    <!-- First section - Welcome screen -->
    <div class="welcome-container">
      <!-- Background image -->
      <div class="background-image"></div>

      <!-- Welcome message -->
      <div class="welcome-message">
        <h1 class="welcome-title">欢迎来到番茄书城</h1>
        <p class="welcome-subtitle">知识如番茄，营养又美味</p>
      </div>

      <!-- Button to navigate -->
      <div class="action-container">
        <button class="action-button" @click="goToProductList">
          开始探索
        </button>
      </div>
    </div>

    <!-- Featured Products Section -->
    <div class="featured-section">
      <div class="featured-products">
        <h2 class="featured-title">热门推荐</h2>
        <div class="products-container" v-loading="loading">
          <ProductCard
              v-for="product in products"
              :key="product.id"
              :product="product"
              :stockpile="stockpiles[product.id] || { amount: 0, frozen: 0 }"
              :cart-items="cartItems"
              @cart-add="handleCartAdd"
              @cart-subtract="handleCartSubtract"
              @cart-updated="handleCartUpdated"
          />
        </div>
      </div>
    </div>

    <!-- Second section - Book recommendation -->
    <div class="booklist-section">
      <div class="booklist-container">
        <!-- Left side - Image -->
        <div class="booklist-image-container">
          <div class="booklist-image"></div>
        </div>

        <!-- Right side - Text and button -->
        <div class="booklist-content">
          <p class="booklist-quote">书是人类进步的阶梯</p>
          <h2 class="booklist-title">看看别人的书单吧</h2>
          <button class="booklist-button" @click="goToBookList">
            查看书单
          </button>
        </div>
      </div>
    </div>

    <!-- Book Lists Recommendation Section -->
    <div class="recommended-booklists-section">
      <div class="section-container">
        <h2 class="section-title">精选书单</h2>
        <div class="booklists-container" v-loading="loadingBookLists">
          <div v-if="bookLists.length > 0" class="booklists-grid">
            <div v-for="bookList in bookLists" :key="bookList.id" class="booklist-item-wrapper">
              <BookListItem
                  :book-list="bookList"
                  :isFavourite="favouriteBookListIds.has(bookList.id)"
                  :isCreator="false"
                  @collect="handleBookListCollect"
                  @delete="handleBookListDelete"
                  @view="handleBookListView"
              />
            </div>
          </div>
          <div v-else class="no-data-message">
            暂无书单数据
          </div>
        </div>
      </div>
    </div>

    <!-- New section - Reading Notes -->
    <div class="readingnotes-section">
      <div class="readingnotes-container">
        <!-- Left side - Text and button -->
        <div class="readingnotes-content">
          <p class="readingnotes-quote">读书不觉已春深，一寸光阴一寸金</p>
          <h2 class="readingnotes-title">记录你的阅读心得</h2>
          <button class="readingnotes-button" @click="router.push('/productList?tab=notes')">
            浏览笔记
          </button>
        </div>

        <!-- Right side - Image -->
        <div class="readingnotes-image-container">
          <div class="readingnotes-image"></div>
        </div>
      </div>
    </div>

    <!-- Top Reading Notes Section -->
    <div class="top-notes-section">
      <div class="section-container">
        <h2 class="section-title">热门笔记</h2>
        <div class="notes-container" v-loading="loadingNotes">
          <div v-if="topNotes.length > 0" class="notes-grid">
            <ReadingNote
              v-for="note in topNotes"
              :key="note.id"
              :note="note"
              :isLiked="likedNoteIds.has(note.id)"
              :isCreator="currentUserId === note.creatorId"
              :isPaid="paidNoteIds.has(note.id)"
              @like="handleNoteLike"
              @unlike="handleNoteUnlike"
              @delete="handleNoteDelete"
              @purchase="handleNotePurchase"
              @view="handleViewNote"
            />
          </div>
          <div v-else class="no-data-message">
            暂无笔记数据
          </div>
        </div>
      </div>
    </div>

    <!-- 笔记详情弹窗 -->
    <el-dialog
      v-model="detailNoteDialogVisible"
      title="笔记详情"
      width="600px"
    >
      <div v-if="currentNote" class="note-detail">
        <div class="detail-header">
          <h2>{{ currentNote.title }}</h2>
          <div class="detail-price" :class="{ 'paid': paidNoteIds.has(currentNote.id) }">
            <template v-if="currentNote.price > 0">
              🍅{{ currentNote.price }}
              <span v-if="paidNoteIds.has(currentNote.id)" class="paid-badge">已购买</span>
            </template>
            <span v-else class="free">免费</span>
          </div>
        </div>

        <el-image
          v-if="currentNote.img"
          :src="currentNote.img"
          class="note-image"
          style="max-width: 100%; margin: 10px 0;"
        />

        <div class="note-content-container">
          <div
            class="note-content"
            :class="{ 'limited-content': currentNote.price > 0 && !paidNoteIds.has(currentNote.id) }"
            style="white-space: pre-wrap;"
          >
            {{ getDisplayContent(currentNote.content, paidNoteIds.has(currentNote.id)) }}
          </div>

          <div
            v-if="currentNote.price > 0 && !paidNoteIds.has(currentNote.id)"
            class="purchase-tip"
          >
            <el-alert
              title="预览内容已结束，购买后可查看完整笔记"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-button
              type="primary"
              class="purchase-button"
              @click="handleNotePurchase(currentNote)"
            >
              立即解锁（{{ currentNote.price }} 🍅）
            </el-button>
          </div>
        </div>

        <div class="note-footer">
          <span>浏览量: {{ currentNote.viewCnt }}</span>
          <span>点赞数: {{ currentNote.likeCnt }}</span>
          <span>创建时间: {{ currentNote.createTime }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 购买确认弹窗 -->
    <el-dialog
      v-model="showPurchaseDialog"
      title="确认购买"
      width="500px"
      class="purchase-confirm-dialog"
    >
      <div v-if="selectedNote" class="confirm-purchase">
        <img
          :src="selectedNote.img || '/default-note-cover.png'"
          class="note-cover"
          alt="笔记封面"
        />
        <div class="content">
          <h3>是否确认购买《{{ selectedNote.title }}》？</h3>
          <p class="price">价格：{{ selectedNote.price }} 🍅</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="showPurchaseDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPurchase"
          :loading="confirmLoading"
        >
          确认购买
        </el-button>
      </template>
    </el-dialog>

    <!-- 书单详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="书单详情"
        width="800px"
        :close-on-click-modal="false"
    >
      <div v-if="currentBookList" class="booklist-detail">
        <h2>{{ currentBookList.title }}</h2>
        <p class="description">{{ currentBookList.description }}</p>

        <div class="products-list">
          <div
              v-for="product in currentBookList.products"
              :key="product.id"
              class="product-item"
              @click="handleProductClick(product.id)"
          >
            <img :src="product.cover" :alt="product.title" class="product-cover">
            <div class="product-info">
              <h4>{{ product.title }}</h4>
              <p class="price">¥{{ product.price }}</p>
            </div>
            <!-- 删除按钮已隐藏 -->
          </div>
        </div>

        <div v-if="currentUserId === currentBookList.creatorId" class="add-product">
          <el-select
              v-model="selectedProduct"
              filterable
              placeholder="添加商品到书单"
              style="width: 100%"
          >
            <el-option
                v-for="product in products"
                :key="product.id"
                :label="product.title"
                :value="product.id"
            />
          </el-select>
          <el-button
              type="primary"
              @click="handleAddProduct(currentBookList.id)"
              :disabled="!selectedProduct"
          >
            添加商品
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* Confetti Canvas */
#confetti-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9999;
}

/* Page container */
.page-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100vh;
}

/* First section styles */
.welcome-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

/* Background image */
.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('https://images.unsplash.com/photo-1630343710506-89f8b9f21d31?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
  z-index: 1;
}

/* Overlay to make text more readable */
.background-image::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 2;
}

/* Welcome message */
.welcome-message {
  position: relative;
  z-index: 4;
  text-align: center;
  margin-bottom: 60px;
}

.welcome-title {
  font-size: 48px;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 15px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  font-family: 'Microsoft YaHei', 'Arial', sans-serif;
}

.welcome-subtitle {
  font-size: 20px;
  color: #ffffff;
  font-style: italic;
  margin: 0;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
}

/* Action button container */
.action-container {
  position: absolute;
  bottom: 10%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 4;
  width: 200px;
}

.action-button {
  width: 100%;
  height: 44px;
  background-color: #d9534f; /* Tomato Red */
  border: none;
  color: #ffffff;
  font-weight: 500;
  transition: all 0.3s;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.action-button:hover {
  background-color: #c9302c; /* Darker Tomato Red */
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.4);
}

.action-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* Featured Products Section */
.featured-section {
  padding: 60px 0;
  background-color: #f8f9fa;
  position: relative;
}

.featured-products {
  position: relative;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.featured-title {
  text-align: center;
  color: #333;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 40px;
  position: relative;
  display: inline-block;
  left: 50%;
  transform: translateX(-50%);
}

.featured-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #ff6347, transparent);
}

.products-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width: 100%;
}

/* Second section - Book recommendation */
.booklist-section {
  min-height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #ffffff 0%, #f8f9fa 100%);
  padding: 60px 20px; /* Reduced padding from 80px to 60px */
  margin: 0;
}

.booklist-container {
  display: flex;
  max-width: 1400px;
  width: 100%;
  box-shadow: none;
  border-radius: 0;
  overflow: hidden;
  background-color: transparent;
  border: none;
}

/* Left side - Image */
.booklist-image-container {
  flex: 1.2;
  position: relative;
  min-height: 600px;
}

.booklist-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('https://images.unsplash.com/photo-1614548428893-5fa2cb74a442?q=80&w=996&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
  transition: transform 0.5s ease;
}

.booklist-image-container:hover .booklist-image {
  transform: scale(1.05);
}

/* Right side - Content */
.booklist-content {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.booklist-quote {
  font-size: 18px;
  color: #6c757d;
  margin-bottom: 20px;
  font-style: italic;
  position: relative;
}

.booklist-quote::before,
.booklist-quote::after {
  content: '"';
  font-size: 24px;
  color: #d9534f;
}

.booklist-title {
  font-size: 36px;
  font-weight: bold;
  color: #333;
  margin-bottom: 40px;
  position: relative;
}

.booklist-title::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 3px;
  background-color: #d9534f;
}

.booklist-button {
  padding: 12px 36px;
  background-color: #d9534f;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(217, 83, 79, 0.3);
}

.booklist-button:hover {
  background-color: #c9302c;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(217, 83, 79, 0.4);
}

.booklist-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(217, 83, 79, 0.3);
}

/* Book Lists Recommendation Section */
.recommended-booklists-section {
  padding: 40px 0; /* Reduced padding from 60px to 40px */
  background-color: #f8f9fa;
  position: relative;
}

.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.section-title {
  text-align: center;
  color: #333;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 40px;
  position: relative;
  display: inline-block;
  left: 50%;
  transform: translateX(-50%);
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #ff6347, transparent);
}

.booklists-container {
  width: 100%;
}

.booklists-grid {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 20px;
  overflow-x: auto;
  padding: 10px 0;
}

.booklist-item-wrapper {
  width: 280px;
  min-width: 280px;
  height: 520px;
  flex-shrink: 0;
}

/* 自定义BookListItem在Welcome页面的样式 */
.booklists-grid :deep(.booklist-card) {
  margin: 0;
  height: 100%;
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
}

.booklists-grid :deep(.booklist-card:hover) {
  transform: translateY(-10px);
  box-shadow: 0 16px 32px rgba(217, 83, 79, 0.2);
}

.no-data-message {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 16px;
}

/* New section - Reading Notes */
.readingnotes-section {
  min-height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #f8f9fa 0%, #ffffff 100%);
  padding: 60px 20px;
  margin: 0;
}

.readingnotes-container {
  display: flex;
  max-width: 1400px;
  width: 100%;
  box-shadow: none;
  border-radius: 0;
  overflow: hidden;
  background-color: transparent;
  border: none;
}

/* Left side - Content for reading notes */
.readingnotes-content {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.readingnotes-quote {
  font-size: 18px;
  color: #6c757d;
  margin-bottom: 20px;
  font-style: italic;
  position: relative;
}

.readingnotes-quote::before,
.readingnotes-quote::after {
  content: '"';
  font-size: 24px;
  color: #d9534f;
}

.readingnotes-title {
  font-size: 36px;
  font-weight: bold;
  color: #333;
  margin-bottom: 40px;
  position: relative;
}

.readingnotes-title::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 3px;
  background-color: #d9534f;
}

.readingnotes-button {
  padding: 12px 36px;
  background-color: #d9534f;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(217, 83, 79, 0.3);
}

.readingnotes-button:hover {
  background-color: #c9302c;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(217, 83, 79, 0.4);
}

.readingnotes-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(217, 83, 79, 0.3);
}

/* Right side - Image for reading notes */
.readingnotes-image-container {
  flex: 1.2;
  position: relative;
  min-height: 600px;
}

.readingnotes-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('https://images.unsplash.com/photo-1529158062015-cad636e205a0?q=80&w=906&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D');
  background-size: cover;
  background-position: center;
  transition: transform 0.5s ease;
}

.readingnotes-image-container:hover .readingnotes-image {
  transform: scale(1.05);
}

/* Responsive design */
@media (max-width: 992px) {
  .booklist-container, .readingnotes-container {
    flex-direction: column;
  }

  .booklist-image-container, .readingnotes-image-container {
    min-height: 300px;
  }

  .booklist-content, .readingnotes-content {
    padding: 40px 20px;
  }

  .products-container {
    grid-template-columns: repeat(2, 1fr);
  }

  .booklists-container {
    justify-content: center;
  }

  .booklist-item-wrapper {
    width: 45%;
  }
}

@media (max-width: 768px) {
  .welcome-title {
    font-size: 36px;
  }

  .welcome-subtitle {
    font-size: 16px;
  }

  .action-container {
    width: 180px;
  }

  .booklist-title, .readingnotes-title {
    font-size: 28px;
  }

  .booklist-quote, .readingnotes-quote {
    font-size: 16px;
  }

  .products-container {
    grid-template-columns: 1fr;
  }

  .featured-title, .section-title {
    font-size: 24px;
  }

  .booklist-item-wrapper {
    width: 100%;
  }
}

/* Adjust product card height for this specific page */
:deep(.product-card) {
  height: 420px;
  transition: transform 0.3s ease;
}

:deep(.product-card:hover) {
  transform: translateY(-8px);
}

/* 书单详情样式 */
.booklist-detail {
  padding: 20px 0;
}

.booklist-detail h2 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 16px;
  text-align: center;
}

.booklist-detail .description {
  font-size: 16px;
  color: #666;
  margin-bottom: 24px;
  line-height: 1.6;
  text-align: center;
  max-width: 80%;
  margin-left: auto;
  margin-right: auto;
}

.products-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.product-item {
  position: relative;
  height: 220px;
  border-radius: 12px;
  overflow: hidden;
  background: #f8f9fa;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.product-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
}

.product-cover {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.product-info {
  padding: 10px;
}

.product-info h4 {
  font-size: 14px;
  font-weight: bold;
  margin: 0 0 6px 0;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-info .price {
  font-size: 14px;
  color: #d9534f;
  font-weight: 500;
  margin: 0;
}

.add-product {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

/* 弹窗样式 */
:deep(.el-dialog) {
  background: rgba(255,255,255,0.72) !important;
  backdrop-filter: blur(18px) !important;
  -webkit-backdrop-filter: blur(18px) !important;
  border-radius: 24px !important;
  box-shadow: 0 8px 40px rgba(0,0,0,0.18);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 20px 10px;
}

:deep(.el-dialog__title) {
  font-weight: bold;
  color: #333;
  font-size: 20px;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 10px 20px 20px;
  text-align: center;
}

:deep(.el-button) {
  border-radius: 8px;
}

:deep(.el-button--primary) {
  background-color: #d9534f;
  border-color: #d9534f;
}

:deep(.el-button--primary:hover) {
  background-color: #c9302c;
  border-color: #c9302c;
}

/* Top Reading Notes Section */
.top-notes-section {
  padding: 60px 0;
  background-color: #fff;
  position: relative;
}

.notes-container {
  width: 100%;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  padding: 10px 0;
}

@media (max-width: 1200px) {
  .notes-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .notes-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .notes-grid {
    grid-template-columns: 1fr;
  }
}

/* 笔记详情样式 */
.note-detail {
  padding: 20px 0;
}

.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.detail-header h2 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
  text-align: center;
}

.detail-price {
  font-size: 18px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-price.paid {
  color: #67c23a;
}

.paid-badge {
  background: #f0f9eb;
  color: #67c23a;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.free {
  color: #909399;
}

.note-content-container {
  margin: 20px 0;
}

.note-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.limited-content {
  position: relative;
  overflow: hidden;
}

.purchase-tip {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.purchase-button {
  width: 100%;
  max-width: 250px;
  margin: 0 auto;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
  color: #909399;
  font-size: 14px;
}

/* 购买确认弹窗样式 */
.confirm-purchase {
  display: flex;
  align-items: center;
  gap: 20px;
}

.note-cover {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}

.confirm-purchase .content {
  flex: 1;
}

.confirm-purchase h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.confirm-purchase .price {
  font-size: 16px;
  color: #e6a23c;
  font-weight: 500;
}
</style>