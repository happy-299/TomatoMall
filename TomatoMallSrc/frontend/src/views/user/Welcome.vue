<script setup lang="ts">
import { onMounted, ref } from 'vue';
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

// Confetti animation setup
onMounted(() => {
  createConfetti();
  fetchProducts();
});

const createConfetti = () => {
  const confettiCount = 150;
  const colors = ['#FF6347', '#FF8C69', '#FFA07A', '#FF7F50', '#FA8072', '#28a745', '#20c997', '#ffc107'];
  
  const container = document.querySelector('.confetti-container');
  if (!container) return;

  for (let i = 0; i < confettiCount; i++) {
    const confetti = document.createElement('div');
    confetti.className = 'confetti';
    confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
    confetti.style.left = `${Math.random() * 100}%`;
    confetti.style.top = `${Math.random() * 100}%`;
    confetti.style.transform = `scale(${Math.random() * 0.7 + 0.3})`;
    confetti.style.animationDuration = `${Math.random() * 3 + 2}s`;
    confetti.style.animationDelay = `${Math.random() * 2}s`;
    container.appendChild(confetti);
  }
};

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
    <!-- First section - Welcome screen -->
    <div class="welcome-container">
      <!-- Background image -->
      <div class="background-image"></div>
      
      <!-- Confetti animation container -->
      <div class="confetti-container"></div>
      
      <!-- Decorative elements -->
      <div class="decorations">
        <div class="floating-book book-1">📖</div>
        <div class="floating-book book-2">📗</div>
        <div class="floating-book book-3">📘</div>
        <div class="floating-tomato tomato-1">🍅</div>
        <div class="floating-tomato tomato-2">🍅</div>
        <div class="floating-leaf leaf-1">🌿</div>
        <div class="floating-leaf leaf-2">🌱</div>
      </div>
      
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

/* Confetti animation */
.confetti-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 3;
  pointer-events: none;
}

.confetti {
  position: absolute;
  width: 10px;
  height: 10px;
  background-color: #ff6347;
  border-radius: 2px;
  animation: confettiDrop 3s ease-in-out forwards;
  opacity: 0;
  transform-origin: center;
}

@keyframes confettiDrop {
  0% {
    opacity: 1;
    transform: translateY(-100vh) rotate(0deg);
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(100vh) rotate(720deg);
  }
}

/* Decorative elements */
.decorations {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 3;
}

.floating-book, .floating-tomato, .floating-leaf {
  position: absolute;
  font-size: 24px;
  animation: float-around 6s ease-in-out infinite;
  z-index: 3;
}

.floating-book {
  animation-duration: 8s;
}

.floating-tomato {
  animation-duration: 7s;
}

.floating-leaf {
  animation-duration: 9s;
}

@keyframes float-around {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(20px, -20px) rotate(90deg);
  }
  50% {
    transform: translate(-10px, -30px) rotate(180deg);
  }
  75% {
    transform: translate(-20px, -10px) rotate(270deg);
  }
}

.book-1 { top: 20%; left: 15%; animation-delay: 0s; }
.book-2 { top: 60%; left: 25%; animation-delay: -2s; }
.book-3 { top: 80%; left: 10%; animation-delay: -4s; }
.tomato-1 { top: 30%; right: 20%; animation-delay: -1s; }
.tomato-2 { top: 70%; right: 15%; animation-delay: -3s; }
.leaf-1 { top: 40%; left: 5%; animation-delay: -1.5s; }
.leaf-2 { top: 90%; right: 25%; animation-delay: -2.5s; }

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

/* Second section - Book recommendation */
.booklist-section {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #ffffff 0%, #f8f9fa 100%);
  padding: 40px 20px;
}

.booklist-container {
  display: flex;
  max-width: 1200px;
  width: 100%;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
  background-color: white;
}

/* Left side - Image */
.booklist-image-container {
  flex: 1;
  position: relative;
  min-height: 500px;
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