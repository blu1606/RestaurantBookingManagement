import json
from typing import Dict, Any
try:
    from ..core.base_agent import BaseAgent
except ImportError:
    try:
        from agents.core.base_agent import BaseAgent
    except ImportError:
        # Fallback for direct execution
        from base_agent import BaseAgent

class MenuAgent(BaseAgent):
    """
    AI Agent chuyên xử lý gợi ý món ăn và thông tin menu
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="MenuAgent",
            data_files=["menu_items.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là chuyên gia ẩm thực của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Giới thiệu và mô tả các món ăn trong menu
        2. Gợi ý món ăn phù hợp với sở thích và nhu cầu của khách hàng
        3. Trả lời các câu hỏi về thành phần, cách chế biến, giá cả
        4. Đề xuất món ăn kết hợp hoặc set menu
        5. Giải thích về món ăn đặc biệt hoặc bán chạy
        
        Luôn cung cấp thông tin chính xác về giá cả, thành phần và đặc điểm món ăn.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base (menu items)
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin menu hiện tại:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu khách hàng muốn xem menu:
        {{
            "action": "show_menu",
            "parameters": {{}},
            "naturalResponse": "Câu trả lời giới thiệu menu"
        }}
        
        Nếu khách hàng muốn gợi ý món:
        {{
            "action": "recommend_food",
            "parameters": {{
                "preferences": "sở thích được đề cập",
                "budget": "ngân sách nếu có",
                "dietary": "yêu cầu ăn kiêng nếu có"
            }},
            "naturalResponse": "Câu trả lời gợi ý món"
        }}
        
        Nếu khách hàng hỏi về món cụ thể:
        {{
            "action": "food_info",
            "parameters": {{
                "foodName": "tên món",
                "details": "chi tiết cần biết"
            }},
            "naturalResponse": "Câu trả lời về món ăn"
        }}
        
        Nếu khách hàng muốn đặt món:
        {{
            "action": "order_food",
            "parameters": {{
                "foodName": "tên món",
                "quantity": "số lượng"
            }},
            "naturalResponse": "Câu trả lời xác nhận đặt món"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text,
            fallback_action="show_menu",
            fallback_response="Đây là menu của nhà hàng. Bạn có muốn tôi gợi ý món nào ngon không?"
        )
        
        return self.create_response(
            action=response_data.get("action", "show_menu"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Đây là menu của nhà hàng chúng tôi.")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format menu knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "menu_item":
                return f"Món: {item.get('name', '')} - Giá: {item.get('price', '')} VND - Mô tả: {item.get('description', '')}"
            elif item.get("type") == "category":
                return f"Danh mục: {item.get('name', '')} - {item.get('description', '')}"
            elif item.get("type") == "special":
                return f"Món đặc biệt: {item.get('name', '')} - {item.get('description', '')}"
        return super()._format_knowledge_item(item) 