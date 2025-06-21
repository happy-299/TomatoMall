<script setup lang="ts">
import { onMounted, ref, nextTick, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getProducts, type Product, getStockpile } from '../../api/product';
import ProductCard from '../../components/ProductCard.vue';

const router = useRouter();
const products = ref<Product[]>([]);
const stockpiles = ref<Record<string, { amount: number, frozen: number }>>({});
const loading = ref(true);
const cartItems = ref<Record<string, { cartItemId: string; quantity: number }>>({});

// Navigate to product list page
const goToProductList = () => {
  router.push({ path: "/productList" });
};

// Navigate to booklist page
const goToBookList = () => {
  router.push({ path: "/productList" });
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

// Initialize data and animations
onMounted(() => {
  fetchProducts();
  initConfetti();
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
  padding: 80px 20px;
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

/* Responsive design */
@media (max-width: 992px) {
  .booklist-container {
    flex-direction: column;
  }
  
  .booklist-image-container {
    min-height: 300px;
  }
  
  .booklist-content {
    padding: 40px 20px;
  }
  
  .products-container {
    grid-template-columns: repeat(2, 1fr);
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
  
  .booklist-title {
    font-size: 28px;
  }
  
  .booklist-quote {
    font-size: 16px;
  }
  
  .products-container {
    grid-template-columns: 1fr;
  }
  
  .featured-title {
    font-size: 24px;
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
</style>