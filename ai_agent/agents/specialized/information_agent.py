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

class InformationAgent(BaseAgent):
    """
    AI Agent chuyên cung cấp thông tin về nhà hàng
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="InformationAgent",
            data_files=["tables.json", "menu_items.json", "customers.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên cung cấp thông tin chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Cung cấp thông tin về địa chỉ, giờ mở cửa, số điện thoại
        2. Giải thích về các dịch vụ và tiện ích của nhà hàng
        3. Hướng dẫn đường đi và phương tiện di chuyển
        4. Trả lời các câu hỏi về không gian, trang thiết bị
        5. Cung cấp thông tin về sự kiện đặc biệt hoặc khuyến mãi
        
        Luôn cung cấp thông tin chính xác và hữu ích cho khách hàng.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về nhà hàng:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu khách hàng hỏi về địa chỉ:
        {{
            "action": "restaurant_address",
            "parameters": {{
                "address": "địa chỉ nhà hàng",
                "landmark": "địa điểm tham chiếu"
            }},
            "naturalResponse": "Câu trả lời về địa chỉ"
        }}
        
        Nếu khách hàng hỏi về giờ mở cửa:
        {{
            "action": "opening_hours",
            "parameters": {{
                "hours": "giờ mở cửa",
                "days": "ngày hoạt động"
            }},
            "naturalResponse": "Câu trả lời về giờ mở cửa"
        }}
        
        Nếu khách hàng hỏi về liên hệ:
        {{
            "action": "contact_info",
            "parameters": {{
                "phone": "số điện thoại",
                "email": "email nếu có"
            }},
            "naturalResponse": "Câu trả lời về thông tin liên hệ"
        }}
        
        Nếu khách hàng hỏi về dịch vụ:
        {{
            "action": "services_info",
            "parameters": {{
                "services": "danh sách dịch vụ"
            }},
            "naturalResponse": "Câu trả lời về dịch vụ"
        }}
        
        Nếu khách hàng hỏi về hướng dẫn đường đi:
        {{
            "action": "directions",
            "parameters": {{
                "transportation": "phương tiện di chuyển",
                "route": "hướng dẫn đường đi"
            }},
            "naturalResponse": "Câu trả lời hướng dẫn đường đi"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text,
            fallback_action="general_info",
            fallback_response="Tôi sẽ cung cấp thông tin về nhà hàng cho bạn. Bạn muốn biết thông tin gì cụ thể?"
        )
        
        return self.create_response(
            action=response_data.get("action", "general_info"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ cung cấp thông tin về nhà hàng cho bạn.")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format information knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "address":
                return f"Địa chỉ: {item.get('street', '')}, {item.get('district', '')}, {item.get('city', '')}"
            elif item.get("type") == "hours":
                return f"Giờ mở cửa: {item.get('open', '')} - {item.get('close', '')} ({item.get('days', '')})"
            elif item.get("type") == "contact":
                return f"Liên hệ: {item.get('phone', '')} - {item.get('email', '')}"
            elif item.get("type") == "service":
                return f"Dịch vụ: {item.get('name', '')} - {item.get('description', '')}"
        return super()._format_knowledge_item(item) 