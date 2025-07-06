import google.generativeai as genai
import os
import json
from typing import Dict, List, Optional
from enum import Enum

# --- Cấu hình Gemini API Key ---
# Đảm bảo bạn đã đặt biến môi trường GOOGLE_API_KEY
# Hoặc bạn có thể thay thế os.getenv("GOOGLE_API_KEY") bằng API key của bạn trực tiếp (KHÔNG NÊN LÀM TRONG MÔI TRƯỜNG SẢN XUẤT)
try:
    genai.configure(api_key=os.getenv("GOOGLE_API_KEY"))
except Exception as e:
    print(f"Lỗi cấu hình Gemini API: {e}")
    print("Vui lòng đảm bảo bạn đã đặt biến môi trường 'GOOGLE_API_KEY' hoặc cung cấp API key.")
    exit()

class Intent(Enum):
    GREETING = "greeting"
    MENU_RECOMMENDATION = "menu_recommendation"
    BOOKING = "booking"
    CANCELLATION_MODIFICATION = "cancellation_modification"
    RESTAURANT_INFO = "restaurant_info"
    FEEDBACK = "feedback"
    MANAGEMENT = "management"
    FALLBACK = "fallback"

class RouterAI:
    """
    Router AI - Phân tích ý định và điều hướng đến các AI Agent chuyên biệt sử dụng Gemini API.
    """
    
    def __init__(self, model_name: str = "gemini-2.5-flash"):
        self.model = genai.GenerativeModel(model_name)
        
        # Định nghĩa các Intent và các ví dụ cho Gemini
        # Đây là phần quan trọng để Gemini hiểu rõ từng Intent
        self.intent_definitions = {
            Intent.GREETING.value: {
                "description": "Người dùng đang chào hỏi, giới thiệu bản thân hoặc hỏi thông tin chung về AI.",
                "examples": [
                    "Chào bạn", "Hello", "Hi", "Xin chào", "Bạn là ai?", "Bạn có khỏe không?",
                    "Giới thiệu về bạn đi", "Bạn có thể giúp gì?", "Chào buổi sáng", "Chào buổi tối"
                ]
            },
            Intent.MENU_RECOMMENDATION.value: {
                "description": "Người dùng muốn biết về thực đơn, các món ăn đặc biệt, hoặc cần gợi ý món ăn.",
                "examples": [
                    "Món nào ngon nhất ở nhà hàng?", "Bạn có thể gợi ý món ăn không?", "Thực đơn hôm nay có gì?",
                    "Có món chay không?", "Món đặc biệt của nhà hàng là gì?", "Cho xem menu", "Tôi nên ăn gì?"
                ]
            },
            Intent.BOOKING.value: {
                "description": "Người dùng muốn đặt bàn, kiểm tra bàn trống hoặc hỏi về khả năng đặt chỗ.",
                "examples": [
                    "Tôi muốn đặt bàn cho 4 người vào 7 giờ tối", "Còn bàn trống vào ngày mai không?",
                    "Đặt bàn cho 2 người vào lúc 6h chiều", "Có thể đặt chỗ vào cuối tuần này không?"
                ]
            },
            Intent.CANCELLATION_MODIFICATION.value: {
                "description": "Người dùng muốn hủy hoặc thay đổi thông tin đặt bàn đã có.",
                "examples": [
                    "Tôi muốn hủy đặt bàn số 123", "Đổi giờ đặt bàn từ 7h sang 8h",
                    "Thay đổi số người trong đặt bàn của tôi", "Hủy reservation của tôi"
                ]
            },
            Intent.RESTAURANT_INFO.value: {
                "description": "Người dùng hỏi thông tin về nhà hàng như địa chỉ, giờ mở cửa, số điện thoại.",
                "examples": [
                    "Nhà hàng mở cửa đến mấy giờ?", "Địa chỉ nhà hàng ở đâu?", "Số điện thoại liên hệ là gì?",
                    "Giờ làm việc của quán là khi nào?"
                ]
            },
            Intent.FEEDBACK.value: {
                "description": "Người dùng đưa ra phản hồi, đánh giá hoặc bày tỏ cảm xúc về trải nghiệm.",
                "examples": [
                    "Món ăn rất ngon!", "Dịch vụ hơi chậm.", "Tôi rất hài lòng", "Chỗ này tệ quá",
                    "Cảm ơn bạn", "Tôi muốn đóng góp ý kiến"
                ]
            },
            Intent.MANAGEMENT.value: {
                "description": "Người dùng thực hiện các tác vụ quản lý nhà hàng như thêm/xóa/cập nhật menu, bàn, khách hàng, đơn hàng.",
                "examples": [
                    "Thêm món phở bò giá 50000 vào menu", "Xóa món có ID 3", "Cập nhật giá món ăn",
                    "Thêm bàn 6 người", "Xóa bàn 3", "Tạo khách hàng mới", "Xem doanh thu",
                    "Hoàn thành đơn hàng 5", "Tính tiền cho booking 10", "Hủy đặt bàn 8"
                ]
            },
            Intent.FALLBACK.value: {
                "description": "Người dùng hỏi những câu không liên quan, không rõ ràng hoặc không thuộc các intent trên.",
                "examples": [
                    "Thời tiết hôm nay thế nào?", "Hôm nay là ngày mấy?", "Bạn có biết về bóng đá không?",
                    "Kể cho tôi một câu chuyện cười", "Bạn làm được gì nữa?"
                ]
            }
        }
        
        # Mapping từ Intent đến Agent
        self.intent_to_agent = {
            Intent.GREETING: "GreetingAgent",
            Intent.MENU_RECOMMENDATION: "MenuAgent", 
            Intent.BOOKING: "BookingAgent",
            Intent.CANCELLATION_MODIFICATION: "CancellationAgent",
            Intent.RESTAURANT_INFO: "InformationAgent",
            Intent.FEEDBACK: "FeedbackAgent",
            Intent.MANAGEMENT: "OrderAgent",
            Intent.FALLBACK: "FallbackAgent"
        }
    
    def _build_gemini_prompt(self, user_input: str) -> str:
        """
        Xây dựng prompt hoàn chỉnh cho Gemini API với cải tiến để đảm bảo output nhất quán.
        """
        prompt_parts = [
            "Bạn là một AI Router chuyên phân loại ý định (intent) của người dùng.",
            "QUAN TRỌNG: Bạn CHỈ được trả về tên intent, KHÔNG có bất kỳ ký tự, dấu câu, hoặc văn bản bổ sung nào.",
            "Dưới đây là danh sách các ý định có thể có, mô tả của chúng và các ví dụ:",
            "---"
        ]

        for intent_name, data in self.intent_definitions.items():
            prompt_parts.append(f"Intent: {intent_name.upper()}")
            prompt_parts.append(f"Mô tả: {data['description']}")
            prompt_parts.append(f"Ví dụ: {'; '.join(data['examples'])}")
            prompt_parts.append("---")
        
        prompt_parts.extend([
            "Nhiệm vụ của bạn là phân loại ý định chính xác nhất cho câu hỏi của người dùng.",
            "QUY TẮC QUAN TRỌNG:",
            "1. Chỉ trả về tên intent (GREETING, BOOKING, MENU_RECOMMENDATION, CANCELLATION_MODIFICATION, RESTAURANT_INFO, FEEDBACK, MANAGEMENT, FALLBACK)",
            "2. KHÔNG thêm dấu ngoặc kép, dấu chấm, hoặc bất kỳ ký tự nào khác",
            "3. KHÔNG viết hoa toàn bộ nếu không phải tên intent",
            "4. Nếu không chắc chắn hoặc câu hỏi không phù hợp, trả về FALLBACK",
            "5. Nếu câu hỏi liên quan đến nhiều intent, chọn intent chính nhất",
            "",
            "User Query: " + user_input,
            "",
            "Intent:"
        ])
        
        return "\n".join(prompt_parts)

    def analyze_intent(self, user_input: str) -> Intent:
        """
        Phân tích ý định của người dùng bằng cách gọi Gemini API.
        """
        prompt = self._build_gemini_prompt(user_input)
        
        try:
            # Gửi prompt đến Gemini
            response = self.model.generate_content(prompt)
            # Lấy kết quả text từ phản hồi
            gemini_output = response.text.strip().upper()
            
            # Chuyển đổi output của Gemini thành Intent Enum
            try:
                # Gemini có thể trả về intent dưới dạng uppercase
                return Intent[gemini_output]
            except KeyError:
                # Nếu Gemini trả về một chuỗi không hợp lệ, mặc định là FALLBACK
                print(f"Cảnh báo: Gemini trả về intent không hợp lệ '{gemini_output}'. Mặc định là FALLBACK.")
                return Intent.FALLBACK
                
        except Exception as e:
            print(f"Lỗi khi gọi Gemini API: {e}")
            # Trong trường hợp có lỗi API, trả về FALLBACK
            return Intent.FALLBACK
    
    def route_to_agent(self, user_input: str) -> Dict[str, str]:
        """
        Điều hướng yêu cầu đến AI Agent phù hợp.
        """
        intent = self.analyze_intent(user_input)
        agent_name = self.intent_to_agent[intent]
        
        
        confidence = 1.0 if intent != Intent.FALLBACK else 0.5 
        
        return {
            "intent": intent.value,
            "agent": agent_name,
            "confidence": confidence,  # Return as float
            "user_input": user_input
        }


