package com.example.data.local

import com.example.data.model.Category
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.SavedAddress
import com.example.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CatalogSeeder {

    val categories = listOf(
        Category("all", "All Styles", "all_inclusive", "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=800&q=80", 24),
        Category("mens", "Men's Clothing", "male", "https://images.unsplash.com/photo-1617137984095-74e4e5e3613f?w=800&q=80", 12),
        Category("womens", "Women's Clothing", "female", "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&q=80", 14),
        Category("tshirts", "T-Shirts", "checkroom", "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&q=80", 6),
        Category("shirts", "Shirts", "styler", "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800&q=80", 6),
        Category("trousers", "Trousers", "straighten", "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&q=80", 5),
        Category("jeans", "Jeans", "content_cut", "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=800&q=80", 5),
        Category("hoodies", "Hoodies", "dry_cleaning", "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=800&q=80", 4),
        Category("jackets", "Jackets", "layers", "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80", 5),
        Category("dresses", "Dresses", "wb_iridescent", "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80", 6),
        Category("sportswear", "Sportswear", "fitness_center", "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&q=80", 4),
        Category("shoes", "Shoes", "roller_skating", "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&q=80", 4),
        Category("accessories", "Accessories", "watch", "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80", 5),
        Category("new_arrivals", "New Arrivals", "fiber_new", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800&q=80", 8),
        Category("sale", "Sale Items", "local_offer", "https://images.unsplash.com/photo-1445205170230-053b83016050?w=800&q=80", 7)
    )

    fun getInitialProducts(): List<Product> = listOf(
        Product(
            id = "prod_001",
            name = "Silk-Blend Double Breasted Blazer",
            description = "Elegantly tailored from a premier mulberry silk and virgin wool blend. Features hand-finished peak lapels, horn buttons, and breathable silk lining.",
            categoryId = "jackets",
            priceLkr = 38500.0,
            discountPriceLkr = 32500.0,
            discountPercent = 15,
            sizesCsv = "S, M, L, XL",
            coloursCsv = "Onyx Black, Champagne Beige, Midnight Navy",
            imageUrlsCsv = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=800&q=80,https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&q=80",
            stock = 14,
            brand = "Lavina Atelier",
            rating = 4.9,
            reviewCount = 42,
            isFeatured = true,
            isNewArrival = true,
            isBestSeller = true,
            isDiscounted = true
        ),
        Product(
            id = "prod_002",
            name = "Aura Heavyweight Cotton Tee",
            description = "Crafted from 280 GSM luxury combed cotton with dropped shoulders and a structured drape that retains its pristine silhouette wash after wash.",
            categoryId = "tshirts",
            priceLkr = 6900.0,
            discountPriceLkr = null,
            discountPercent = 0,
            sizesCsv = "XS, S, M, L, XL, XXL",
            coloursCsv = "Raw Ivory, Deep Noir, Sand Beige, Washed Slate",
            imageUrlsCsv = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&q=80,https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800&q=80",
            stock = 45,
            brand = "Lavina Essentials",
            rating = 4.8,
            reviewCount = 88,
            isFeatured = true,
            isNewArrival = false,
            isBestSeller = true,
            isDiscounted = false
        ),
        Product(
            id = "prod_003",
            name = "Pleated Crepe Evening Gown",
            description = "Graceful floor-sweeping gown designed with micro-accordion pleats, flattering halter neckline, and a fluid silhouette that catches evening light.",
            categoryId = "dresses",
            priceLkr = 44500.0,
            discountPriceLkr = 37900.0,
            discountPercent = 14,
            sizesCsv = "XS, S, M, L",
            coloursCsv = "Emerald Green, Royal Burgundy, Pure Onyx",
            imageUrlsCsv = "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80,https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=800&q=80",
            stock = 8,
            brand = "Lavina Couture",
            rating = 5.0,
            reviewCount = 29,
            isFeatured = true,
            isNewArrival = true,
            isBestSeller = false,
            isDiscounted = true
        ),
        Product(
            id = "prod_004",
            name = "Italian Poplin Crisp Oxford Shirt",
            description = "Woven from premium 120s two-ply Giza cotton with mother-of-pearl buttons and a classic semi-spread collar. Ideal for smart tailoring or effortless weekends.",
            categoryId = "shirts",
            priceLkr = 14800.0,
            discountPriceLkr = 12500.0,
            discountPercent = 15,
            sizesCsv = "S, M, L, XL, XXL",
            coloursCsv = "Crisp White, Ice Blue, Soft Rose",
            imageUrlsCsv = "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800&q=80,https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&q=80",
            stock = 28,
            brand = "Lavina Atelier",
            rating = 4.7,
            reviewCount = 54,
            isFeatured = false,
            isNewArrival = true,
            isBestSeller = true,
            isDiscounted = true
        ),
        Product(
            id = "prod_005",
            name = "Tailored Wide-Leg Wool Trousers",
            description = "High-rise relaxed silhouette cut with front double pleats and an extended waistband tab. Offers structural drape with natural stretch.",
            categoryId = "trousers",
            priceLkr = 22400.0,
            discountPriceLkr = null,
            discountPercent = 0,
            sizesCsv = "28, 30, 32, 34, 36",
            coloursCsv = "Charcoal, Camel, Midnight Blue",
            imageUrlsCsv = "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&q=80",
            stock = 19,
            brand = "Lavina Sartorial",
            rating = 4.8,
            reviewCount = 37,
            isFeatured = true,
            isNewArrival = false,
            isBestSeller = true,
            isDiscounted = false
        ),
        Product(
            id = "prod_006",
            name = "Selvedge Raw Indigo Denim Jeans",
            description = "13.5 oz Japanese shuttle-loom woven denim with genuine red-line selvedge ID, antique copper rivets, and a classic straight modern leg.",
            categoryId = "jeans",
            priceLkr = 19900.0,
            discountPriceLkr = 16900.0,
            discountPercent = 15,
            sizesCsv = "30, 32, 34, 36",
            coloursCsv = "Raw Indigo, Vintage Black, Stonewash Blue",
            imageUrlsCsv = "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=800&q=80",
            stock = 22,
            brand = "Lavina Denim",
            rating = 4.9,
            reviewCount = 63,
            isFeatured = false,
            isNewArrival = false,
            isBestSeller = true,
            isDiscounted = true
        ),
        Product(
            id = "prod_007",
            name = "Cashmere-Cotton Relaxed Hoodie",
            description = "Luxurious blend of 15% Mongolian cashmere and 85% long-staple organic cotton. Detailed with clean seamless kangaroo pocket and ribbed hems.",
            categoryId = "hoodies",
            priceLkr = 18500.0,
            discountPriceLkr = null,
            discountPercent = 0,
            sizesCsv = "S, M, L, XL",
            coloursCsv = "Heather Oatmeal, Noir, Forest Green",
            imageUrlsCsv = "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=800&q=80",
            stock = 16,
            brand = "Lavina Essentials",
            rating = 4.9,
            reviewCount = 47,
            isFeatured = true,
            isNewArrival = true,
            isBestSeller = false,
            isDiscounted = false
        ),
        Product(
            id = "prod_008",
            name = "Leather Chelsea Boots - Handcrafted",
            description = "Full-grain calfskin leather boots with Blake-stitched leather soles, elasticated side gussets, and pull tabs for timeless urban stepping.",
            categoryId = "shoes",
            priceLkr = 32000.0,
            discountPriceLkr = 27500.0,
            discountPercent = 14,
            sizesCsv = "40, 41, 42, 43, 44, 45",
            coloursCsv = "Cognac Tan, Jet Black, Espresso",
            imageUrlsCsv = "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&q=80",
            stock = 11,
            brand = "Lavina Footwear",
            rating = 4.9,
            reviewCount = 31,
            isFeatured = false,
            isNewArrival = true,
            isBestSeller = true,
            isDiscounted = true
        ),
        Product(
            id = "prod_009",
            name = "Heritage Wool Trench Coat",
            description = "Iconic trench silhouette featuring water-resistant bonded wool, storm flap, horn buckles, and a detachable belt with gold-toned metal D-rings.",
            categoryId = "jackets",
            priceLkr = 49000.0,
            discountPriceLkr = 41600.0,
            discountPercent = 15,
            sizesCsv = "S, M, L, XL",
            coloursCsv = "Classic Camel, Jet Noir, Olive Drab",
            imageUrlsCsv = "https://images.unsplash.com/photo-1544441893-675973e31985?w=800&q=80",
            stock = 9,
            brand = "Lavina Couture",
            rating = 5.0,
            reviewCount = 18,
            isFeatured = true,
            isNewArrival = true,
            isBestSeller = false,
            isDiscounted = true
        ),
        Product(
            id = "prod_010",
            name = "Seamless High-Performance Training Set",
            description = "Sweat-wicking compression fabric with four-way stretch, contoured ribbed panels, and flatlock anti-chafing seams for studio and athletic movement.",
            categoryId = "sportswear",
            priceLkr = 16200.0,
            discountPriceLkr = null,
            discountPercent = 0,
            sizesCsv = "XS, S, M, L",
            coloursCsv = "Sage Green, Midnight Navy, Charcoal",
            imageUrlsCsv = "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&q=80",
            stock = 24,
            brand = "Lavina Active",
            rating = 4.7,
            reviewCount = 52,
            isFeatured = false,
            isNewArrival = false,
            isBestSeller = true,
            isDiscounted = false
        ),
        Product(
            id = "prod_011",
            name = "Gold Monogram Leather Belt",
            description = "Italian vegetable-tanned leather belt with an interchangeable brushed 24k gold-finish monogram buckle.",
            categoryId = "accessories",
            priceLkr = 11500.0,
            discountPriceLkr = 9900.0,
            discountPercent = 14,
            sizesCsv = "85, 90, 95, 100",
            coloursCsv = "Black Gold, Cognac Gold",
            imageUrlsCsv = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",
            stock = 30,
            brand = "Lavina Accessories",
            rating = 4.8,
            reviewCount = 26,
            isFeatured = false,
            isNewArrival = true,
            isBestSeller = true,
            isDiscounted = true
        ),
        Product(
            id = "prod_012",
            name = "Silk Floral Wrap Sundress",
            description = "Breezy midi-length wrap silhouette with delicate hand-drawn botanical print, flutter sleeves, and adjustable waist tie.",
            categoryId = "dresses",
            priceLkr = 26800.0,
            discountPriceLkr = 22800.0,
            discountPercent = 15,
            sizesCsv = "XS, S, M, L",
            coloursCsv = "Champagne Gold, Ivory Peach",
            imageUrlsCsv = "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800&q=80",
            stock = 15,
            brand = "Lavina Couture",
            rating = 4.9,
            reviewCount = 38,
            isFeatured = true,
            isNewArrival = true,
            isBestSeller = false,
            isDiscounted = true
        )
    )

    fun getInitialReviews(): List<Review> = listOf(
        Review(0, "prod_001", "usr_dilshan", "Dilshan Perera", 5, "Exquisite blazer! The silk-wool blend falls like a dream. Fits perfectly across the shoulders.", "2 days ago", isVerifiedPurchase = true),
        Review(0, "prod_001", "usr_amaya", "Amaya Senanayake", 5, "Purchased for a gala evening in Colombo. Impeccable packaging and swift delivery.", "1 week ago", isVerifiedPurchase = true),
        Review(0, "prod_002", "usr_pasindu_001", "Pasindu Andreew", 5, "The heavyweight cotton has substantial structure. Definitely beats fast fashion tees.", "3 days ago", isVerifiedPurchase = true),
        Review(0, "prod_003", "usr_sachini", "Sachini Jayawardena", 5, "Stunning evening gown. The pleating movement in person is extraordinary.", "4 days ago", isVerifiedPurchase = true),
        Review(0, "prod_004", "usr_kavinda", "Kavinda Fernando", 5, "Superb collar stiffness and mother-of-pearl buttons. Looks top tier with cufflinks.", "2 weeks ago", isVerifiedPurchase = true),
        Review(0, "prod_008", "usr_roshan", "Roshan Silva", 5, "Genuine Blake stitching on Chelsea boots at this price in LKR is unmatched value.", "5 days ago", isVerifiedPurchase = true)
    )

    fun getInitialUser(): User = User(
        uid = "usr_pasindu_001",
        customerId = "CUS-001089",
        fullName = "Pasindu Andreew",
        email = "pasinduandreew@gmail.com",
        phone = "+94 77 123 4567",
        role = "CUSTOMER",
        photoUrl = "",
        createdAt = System.currentTimeMillis() - 86400000L * 30
    )

    fun getAdminUser(): User = User(
        uid = "usr_admin_001",
        customerId = "ADM-000001",
        fullName = "Lavina Atelier Admin",
        email = "admin@lavina.fashion",
        phone = "+94 71 999 8888",
        role = "ADMIN",
        photoUrl = "",
        createdAt = System.currentTimeMillis() - 86400000L * 60
    )

    fun getInitialAddress(userId: String): SavedAddress = SavedAddress(
        userId = userId,
        label = "Home / Residence",
        recipientName = "Pasindu Andreew",
        phone = "+94 77 123 4567",
        street = "No. 42, Lotus Grove Avenue",
        city = "Colombo 07",
        postalCode = "00700",
        isDefault = true
    )

    fun getInitialNotifications(userId: String): List<NotificationItem> {
        val now = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        return listOf(
            NotificationItem(
                userId = userId,
                title = "Welcome to Lavina Atelier",
                message = "Your VIP customer account has been authorized. Enjoy 20% off your first luxury order with code LUXURY20.",
                type = "PROMO",
                dateFormatted = now,
                isRead = false
            ),
            NotificationItem(
                userId = userId,
                title = "Autumn Collection Released",
                message = "Discover our newly curated silk blazers and evening wear pieces before public release.",
                type = "SYSTEM",
                dateFormatted = "Yesterday",
                isRead = true
            )
        )
    }

    fun getInitialOrders(userId: String): List<Order> = listOf(
        Order(
            orderNumber = "ORD-260920-1042",
            userId = userId,
            customerName = "Pasindu Andreew",
            phone = "+94 77 123 4567",
            streetAddress = "No. 42, Lotus Grove Avenue",
            city = "Colombo 07",
            postalCode = "00700",
            deliveryInstructions = "Leave with concierge",
            deliveryMethod = "Express Courier",
            paymentMethod = "Online Card Payment",
            paymentStatus = "Verified",
            orderStatus = "Delivered",
            subtotalLkr = 39400.0,
            deliveryFeeLkr = 0.0,
            discountLkr = 0.0,
            totalLkr = 39400.0,
            itemsSummary = "1x Silk-Blend Double Breasted Blazer (M, Onyx Black)\n1x Aura Heavyweight Cotton Tee (L, Raw Ivory)",
            productIdsCsv = "prod_001,prod_002",
            createdAt = System.currentTimeMillis() - 86400000L * 14
        )
    )
}
