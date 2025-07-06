import json
from typing import Dict, Any

from ..core.base_agent import BaseAgent


class FeedbackAgent(BaseAgent):
    """
    AI Agent chuyên xử lý phản hồi và đánh giá
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="FeedbackAgent",
            data_files=["knowledge/feedback_knowledge.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là người tiếp nhận phản hồi chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Lắng nghe và ghi nhận phản hồi của khách hàng
        2. Phản hồi tích cực và lịch sự với mọi loại phản hồi
        3. Cảm ơn khách hàng đã dành thời gian phản hồi
        4. Ghi nhận các vấn đề và hứa cải thiện nếu cần
        5. Khuyến khích khách hàng quay lại sử dụng dịch vụ
        
        Luôn thể hiện sự quan tâm và cam kết cải thiện dịch vụ.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về xử lý phản hồi:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu khách hàng đưa ra phản hồi tích cực:
        {{
            "action": "positive_feedback",
            "parameters": {{
                "aspect": "khía cạnh được khen",
                "rating": "đánh giá nếu có"
            }},
            "naturalResponse": "Câu trả lời cảm ơn và khuyến khích"
        }}
        
        Nếu khách hàng đưa ra phản hồi tiêu cực:
        {{
            "action": "negative_feedback",
            "parameters": {{
                "issue": "vấn đề được đề cập",
                "severity": "mức độ nghiêm trọng"
            }},
            "naturalResponse": "Câu trả lời xin lỗi và cam kết cải thiện"
        }}
        
        Nếu khách hàng đưa ra gợi ý:
        {{
            "action": "suggestion",
            "parameters": {{
                "suggestion": "gợi ý cụ thể",
                "category": "loại gợi ý"
            }},
            "naturalResponse": "Câu trả lời ghi nhận gợi ý"
        }}
        
        Nếu khách hàng cảm ơn:
        {{
            "action": "thank_you",
            "parameters": {{}},
            "naturalResponse": "Câu trả lời cảm ơn và chào tạm biệt"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text,
            fallback_action="general_feedback",
            fallback_response="Cảm ơn bạn đã phản hồi. Chúng tôi rất trân trọng ý kiến của bạn và sẽ cố gắng cải thiện dịch vụ."
        )
        
        return self.create_response(
            action=response_data.get("action", "general_feedback"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Cảm ơn bạn đã phản hồi. Chúng tôi rất trân trọng ý kiến của bạn.")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format feedback knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "response_template":
                return f"Template: {item.get('category', '')} - {item.get('content', '')}"
            elif item.get("type") == "feedback_category":
                return f"Danh mục: {item.get('name', '')} - {item.get('description', '')}"
            elif item.get("type") == "improvement":
                return f"Cải thiện: {item.get('area', '')} - {item.get('action', '')}"
        return super()._format_knowledge_item(item) 