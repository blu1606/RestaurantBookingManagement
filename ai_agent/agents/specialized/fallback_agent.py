import json
from typing import Dict, Any

from ..core.base_agent import BaseAgent



class FallbackAgent(BaseAgent):
    """
    AI Agent dự phòng để xử lý các yêu cầu không xác định
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="FallbackAgent",
            data_files=["knowledge/fallback_knowledge.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là trợ lý AI tổng quát của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Xử lý các yêu cầu không xác định hoặc phức tạp
        2. Lịch sự xin lỗi khi không hiểu rõ yêu cầu
        3. Đề nghị khách hàng diễn đạt lại hoặc chuyển hướng
        4. Cung cấp thông tin tổng quát về nhà hàng
        5. Hướng dẫn khách hàng đến các dịch vụ phù hợp
        
        Luôn lịch sự và hữu ích, ngay cả khi không thể xử lý yêu cầu cụ thể.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin tổng quát về nhà hàng:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu không hiểu rõ yêu cầu:
        {{
            "action": "clarify",
            "parameters": {{
                "reason": "lý do không hiểu",
                "suggestion": "gợi ý cách diễn đạt lại"
            }},
            "naturalResponse": "Câu trả lời xin lỗi và yêu cầu làm rõ"
        }}
        
        Nếu yêu cầu ngoài phạm vi dịch vụ:
        {{
            "action": "out_of_scope",
            "parameters": {{
                "scope": "phạm vi dịch vụ",
                "alternative": "giải pháp thay thế"
            }},
            "naturalResponse": "Câu trả lời giải thích và đề xuất thay thế"
        }}
        
        Nếu cần chuyển hướng:
        {{
            "action": "redirect",
            "parameters": {{
                "target": "dịch vụ phù hợp",
                "reason": "lý do chuyển hướng"
            }},
            "naturalResponse": "Câu trả lời chuyển hướng"
        }}
        
        Nếu cung cấp thông tin tổng quát:
        {{
            "action": "general_info",
            "parameters": {{
                "info_type": "loại thông tin"
            }},
            "naturalResponse": "Câu trả lời cung cấp thông tin tổng quát"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text,
            fallback_action="clarify",
            fallback_response="Xin lỗi, tôi không hiểu rõ yêu cầu của bạn. Bạn có thể diễn đạt lại hoặc hỏi về đặt bàn, menu, hoặc thông tin nhà hàng không?"
        )
        
        return self.create_response(
            action=response_data.get("action", "clarify"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Xin lỗi, tôi không hiểu rõ yêu cầu của bạn. Bạn có thể diễn đạt lại được không?")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format fallback knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "general_info":
                return f"Thông tin: {item.get('title', '')} - {item.get('content', '')}"
            elif item.get("type") == "service_scope":
                return f"Phạm vi: {item.get('service', '')} - {item.get('description', '')}"
            elif item.get("type") == "help_topic":
                return f"Trợ giúp: {item.get('topic', '')} - {item.get('content', '')}"
        return super()._format_knowledge_item(item) 