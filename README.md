# AI Restaurant Assistant - Hệ thống Quản lý Đặt bàn Nhà hàng

Hệ thống quản lý đặt bàn nhà hàng thông minh với khả năng hiểu ngôn ngữ tự nhiên, được xây dựng theo kiến trúc MVC với Java backend và Python AI Agent.

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    HTTP API    ┌─────────────────┐
│   Java Backend  │ ◄────────────► │  Python AI Agent│
│   (MVC)         │                │   (RAG + LLM)   │
│                 │                │                 │
│ • Model         │                │ • FAISS VectorDB│
│ • View          │                │ • Gemini LLM    │
│ • Controller    │                │ • Flask API     │
└─────────────────┘                └─────────────────┘
```

## 📋 Yêu cầu hệ thống

### Java Backend
- Java 11 hoặc cao hơn
- NetBeans IDE (khuyến nghị)
- Gson library (cho JSON parsing)

### Python AI Agent
- Python 3.8+
- Google Gemini API key
- Các thư viện Python (xem requirements.txt)

## 🚀 Cài đặt và Chạy

### Bước 1: Cài đặt Python AI Agent

1. Di chuyển vào thư mục AI Agent:
```bash
cd ai_agent
```

2. Tạo virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# hoặc
venv\Scripts\activate     # Windows
```

3. Cài đặt dependencies:
```bash
pip install -r requirements.txt
```

4. Cấu hình Gemini API key:
```bash
export GOOGLE_API_KEY="your-gemini-api-key-here"
# hoặc thêm vào file .env
```

5. Chạy AI Agent:
```bash
python app.py
```

AI Agent sẽ chạy trên `http://localhost:5000`

### Bước 2: Chạy Java Backend

1. Mở project trong NetBeans IDE
2. Build project (F11)
3. Chạy file `RestaurantBookingManagement.java` (F6)

## 🎯 Tính năng chính

### 🤖 AI Assistant
- Hiểu ngôn ngữ tự nhiên tiếng Việt
- Đặt bàn thông minh với thu thập thông tin khách hàng
- Gọi món tự động
- Hủy đặt bàn
- Trả lời thắc mắc về menu và dịch vụ

### 📝 Quản lý Đặt bàn
- Đặt bàn theo số người
- Thu thập thông tin khách hàng (tên, số điện thoại)
- Lưu trữ thông tin khách hàng vào database
- Kiểm tra trùng lịch
- Hủy đặt bàn
- Xem danh sách đặt bàn

### 🍽️ Quản lý Đơn hàng
- Gọi món từ menu
- Tính tổng tiền
- Theo dõi trạng thái đơn hàng
- Quản lý inventory

### 📊 Báo cáo
- Doanh thu
- Thống kê đặt bàn
- Báo cáo món ăn phổ biến

## 💬 Cách sử dụng

### Đặt bàn
```
> Tôi muốn đặt bàn 2 người tối nay
> Đặt bàn 4 người vào 7h tối mai
> Cần bàn 8 người cho bữa trưa
```

**Lưu ý:** Khi đặt bàn lần đầu, AI sẽ hỏi thêm thông tin khách hàng:
```
AI: Dạ vâng, để em đặt bàn cho 2 người vào 7 giờ tối nay ạ. 
    Em cần thêm thông tin của anh: tên anh là gì và số điện thoại để liên lạc ạ?

Bạn: Tên tôi là Nguyễn Văn A, số điện thoại 0901234567

AI: Dạ vâng, em đã ghi nhận thông tin của anh Nguyễn Văn A. 
    Em sẽ tiến hành đặt bàn cho 2 người vào 7 giờ tối nay ạ.
```

### Gọi món
```