const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Security and Middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// Rate Limiter
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  message: { error: 'Too many requests from this IP, please try again after 15 minutes.' }
});
app.use('/api/', limiter);

// Mock in-memory/Firestore fallback database for development
let catalog = [
  {
    id: "prod_001",
    name: "Silk-Blend Double Breasted Blazer",
    categoryId: "jackets",
    priceLkr: 38500.0,
    discountPriceLkr: 32500.0,
    stock: 14,
    sizes: ["S", "M", "L", "XL"],
    colours: ["Onyx Black", "Champagne Beige"]
  },
  {
    id: "prod_002",
    name: "Aura Heavyweight Cotton Tee",
    categoryId: "tshirts",
    priceLkr: 6900.0,
    stock: 45,
    sizes: ["XS", "S", "M", "L", "XL", "XXL"],
    colours: ["Raw Ivory", "Deep Noir"]
  }
];

let orders = [];

// Auth Verification Middleware (Firebase ID Token)
const verifyToken = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or invalid token' });
  }
  // In production with Firebase Admin:
  // const token = authHeader.split('Bearer ')[1];
  // const decoded = await admin.auth().verifyIdToken(token);
  // req.user = decoded;
  req.user = { uid: 'usr_pasindu_001', role: 'CUSTOMER', email: 'pasinduandreew@gmail.com' };
  next();
};

// 1. Health & Status
app.get('/api/health', (req, res) => {
  res.json({ status: 'healthy', timestamp: new Date().toISOString(), atelier: 'LUMINA HAUTE COUTURE' });
});

// 2. Categories
app.get('/api/categories', (req, res) => {
  res.json([
    { id: "mens", name: "Men's Clothing" },
    { id: "womens", name: "Women's Clothing" },
    { id: "tshirts", name: "T-Shirts" },
    { id: "shirts", name: "Shirts" },
    { id: "trousers", name: "Trousers" },
    { id: "jeans", name: "Jeans" },
    { id: "hoodies", name: "Hoodies" },
    { id: "jackets", name: "Jackets" },
    { id: "dresses", name: "Dresses" },
    { id: "sportswear", name: "Sportswear" },
    { id: "shoes", name: "Shoes" },
    { id: "accessories", name: "Accessories" }
  ]);
});

// 3. Products with Pagination & Filter
app.get('/api/products', (req, res) => {
  const { category, search, page = 1, limit = 10 } = req.query;
  let results = [...catalog];

  if (category && category !== 'all') {
    results = results.filter(p => p.categoryId === category);
  }
  if (search) {
    results = results.filter(p => p.name.toLowerCase().includes(search.toLowerCase()));
  }

  const startIndex = (page - 1) * limit;
  const paginated = results.slice(startIndex, startIndex + Number(limit));

  res.json({
    total: results.length,
    page: Number(page),
    limit: Number(limit),
    data: paginated
  });
});

// 4. Cart Validation & Order Creation (Server-Side Recalculation)
app.post('/api/orders/checkout', verifyToken, (req, res) => {
  const { items, deliveryAddress, deliveryMethod, paymentMethod, promoCode } = req.body;

  if (!items || !items.length) {
    return res.status(400).json({ error: 'Cart is empty' });
  }

  let subtotal = 0;
  for (const item of items) {
    const liveProduct = catalog.find(p => p.id === item.productId);
    if (!liveProduct) {
      return res.status(404).json({ error: `Product ${item.productId} not found.` });
    }
    if (liveProduct.stock < item.quantity) {
      return res.status(400).json({ error: `Insufficient stock for ${liveProduct.name}.` });
    }
    const unitPrice = liveProduct.discountPriceLkr || liveProduct.priceLkr;
    subtotal += unitPrice * item.quantity;
    // Decrement stock
    liveProduct.stock -= item.quantity;
  }

  // Calculate delivery
  const deliveryFee = deliveryMethod === 'Express Courier' ? 950 : (subtotal >= 10000 ? 0 : 450);

  // Validate discount code
  let discount = 0;
  if (promoCode === 'LUXURY20') discount = subtotal * 0.20;
  if (promoCode === 'WELCOME10') discount = subtotal * 0.10;

  const total = Math.max(0, subtotal - discount + deliveryFee);
  const orderNumber = `ORD-${Date.now().toString().slice(-6)}`;

  const newOrder = {
    orderNumber,
    userId: req.user.uid,
    items,
    deliveryAddress,
    deliveryMethod,
    paymentMethod,
    paymentStatus: paymentMethod === 'Cash on Delivery' ? 'Pending COD' : 'Verified',
    orderStatus: 'Confirmed',
    subtotal,
    deliveryFee,
    discount,
    total,
    currency: 'LKR',
    createdAt: new Date().toISOString()
  };

  orders.push(newOrder);
  res.status(201).json({ success: true, order: newOrder });
});

// 5. Order History
app.get('/api/orders', verifyToken, (req, res) => {
  const userOrders = orders.filter(o => o.userId === req.user.uid);
  res.json({ data: userOrders });
});

app.listen(PORT, () => {
  console.log(`Lavina Fashion API running on port ${PORT}`);
});
