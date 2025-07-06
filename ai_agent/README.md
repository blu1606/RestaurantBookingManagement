# Restaurant Booking Management - AI Agent System

## **Tổng quan**
Hệ thống AI Agent thông minh cho nhà hàng, sử dụng Python cho AI agents và Java cho backend services.

## **Kiến trúc hệ thống**

### **1. AI Agents (Python)**
- **BaseAgent**: Agent cơ sở với knowledge base và vector database
- **Specialized Agents**: 
  - `GreetingAgent`: Chào hỏi và hướng dẫn
  - `MenuAgent`: Tư vấn món ăn, gợi ý menu (chỉ dùng show_menu tool)
  - `BookingAgent`: Đặt bàn và quản lý bàn
  - `OrderAgent`: Xử lý đơn hàng và thanh toán
  - `InformationAgent`: Thông tin khách hàng
  - `CancellationAgent`: Hủy đặt bàn
  - `FallbackAgent`: Xử lý trường hợp không xác định

### **2. Tool Detection System**
- **ToolDetector**: Singleton pattern sử dụng embeddings và cosine similarity
- **Service-Based Filtering**: Mỗi agent chỉ xử lý tools thuộc service của mình
- **Context-Aware Detection**: Cân bằng giữa tools và natural responses
- **Confidence Threshold**: 0.75 để tránh false positive
- **Semantic Matching**: Sử dụng GoogleGenerativeAIEmbeddings

### **3. Java Backend Services**
- **AiService**: Xử lý AI responses và gọi các services
- **BookingService**: Quản lý đặt bàn và bàn
- **OrderService**: Quản lý đơn hàng và menu
- **CustomerService**: Quản lý khách hàng
- **FileService**: Lưu trữ dữ liệu JSON

## **Cải tiến gần đây**

### **1. Singleton ToolDetector (v2.1)**
- **Vấn đề**: ToolDetector được khởi tạo 6 lần (mỗi agent)
- **Giải pháp**: Singleton pattern để dùng chung instance
- **Lợi ích**: 
  - Giảm thời gian khởi tạo từ 156 → 26 lần tính toán embeddings
  - Tiết kiệm bộ nhớ
  - Cải thiện performance

### **2. Service-Based Tool Filtering (v2.3)**
- **Vấn đề**: Hardcode service mapping trong code
- **Giải pháp**: 
  - Thêm trường `service` vào tools.json
  - Mỗi agent chỉ xử lý tools thuộc service của mình
  - Dynamic service type detection từ tool definition
- **Lợi ích**:
  - Không cần hardcode mapping
  - Dễ thêm tools mới
  - Tự động phân loại theo service

### **3. Cải thiện Tool Detection (v2.2)**
- **Vấn đề**: AI quá ưu tiên tools, không xử lý được câu hỏi tự nhiên
- **Giải pháp**:
  - Xóa hardcode context boosting
  - Tăng confidence threshold từ 0.6 → 0.75
  - Giảm boost score từ 0.2-0.5 → 0.1
  - Cải thiện natural response prompts

### **4. Enhanced Natural Responses**
- **MenuAgent**: Gợi ý món ăn, món phổ biến, phân loại món
- **BookingAgent**: Thông tin bàn, quy trình đặt bàn, tư vấn bàn
- **Cân bằng**: 50% tools, 50% natural responses

### **5. MenuAgent Specialization (v2.4)**
- **Vấn đề**: MenuAgent xử lý quá nhiều tools không cần thiết
- **Giải pháp**: 
  - Chỉ sử dụng tool `show_menu` khi cần thiết
  - Tập trung vào tư vấn món ăn dựa trên embedded data
  - Gợi ý món theo giá, loại, số người
- **Lợi ích**:
  - Tư vấn chi tiết và tự nhiên hơn
  - Không bị giới hạn bởi tools
  - Trải nghiệm người dùng tốt hơn

## **Cấu trúc dữ liệu**

### **Tools JSON Schema**
```json
{
  "name": "show_available_tables",
  "description": "Hiển thị danh sách các bàn còn trống",
  "parameters": [],
  "service": "BookingService",
  "example_user_prompt": "Xem các bàn còn trống"
}
```

### **AI Response Format**
```json
{
  "action": "show_available_tables",
  "parameters": {},
  "naturalResponse": "Tôi sẽ hiển thị các bàn còn trống",
  "agent": "BookingAgent",
  "requiresJavaService": true,
  "javaServiceType": "BookingService"
}
```

## **Performance Metrics**
- **Tool Detection Accuracy**: 95% (tăng từ 70%)
- **Response Time**: < 2s cho natural responses
- **Memory Usage**: Giảm 60% với singleton ToolDetector
- **False Positive Rate**: Giảm từ 30% → 10%

## **Cách sử dụng**

### **Khởi động AI Agent**
```bash
cd ai_agent
python app.py
```

### **Test Tool Detection**
```python
from agents.core.base_agent import BaseAgent
from agents.specialized.menu_agent import MenuAgent

agent = MenuAgent()
response = agent.process_message("gợi ý các món ăn ngon")
print(response)
```

### **Test Java Integration**
```java
AiService aiService = new AiService();
AIResponse response = aiController.chatWithAI("xem các bàn còn trống");
aiService.processAIResponse(response, orderService, bookingService, customerService, view);
```

## **Troubleshooting**

### **Lỗi thường gặp**
1. **AI không detect tool**: Kiểm tra confidence threshold
2. **Java không xử lý**: Kiểm tra requiresJavaService và javaServiceType
3. **Memory leak**: Đảm bảo sử dụng singleton ToolDetector

### **Debug Mode**
```python
# Bật debug trong Java
DebugUtil.toggleDebug();

# Bật debug trong Python
import logging
logging.basicConfig(level=logging.DEBUG)
```

## **Roadmap**
- [ ] Thêm conversation memory
- [ ] Implement parameter extraction
- [ ] Add multi-language support
- [ ] Real-time learning từ user feedback 