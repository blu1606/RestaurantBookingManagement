import json
from typing import Dict, Any, Optional
from ..core.base_agent import BaseAgent

from ..utils import handle_pending_action_utils

class MenuAgent(BaseAgent):
    """
    AI Agent chuyên xử lý gợi ý món ăn và thông tin menu
    """
    
    def __init__(self, gemini_model=None, user_role: str = "user"):
        super().__init__(
            agent_name="MenuAgent",
            data_files=["menu_items.json"],
            gemini_model=gemini_model,
            user_role=user_role
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên tư vấn menu chuyên nghiệp của nhà hàng Việt Nam.
        
        Nhiệm vụ chính của bạn là:
        1. Gợi ý món ăn phù hợp với sở thích và nhu cầu khách hàng
        2. Cung cấp thông tin chi tiết về món ăn (nguyên liệu, cách chế biến, giá cả)
        3. Phân loại món ăn theo loại (chính, tráng miệng, đồ uống)
        4. Giới thiệu các món đặc sản và món phổ biến
        5. Tư vấn món ăn theo tầm giá và số lượng người
        6. Chỉ sử dụng tool show_menu khi khách hàng yêu cầu xem toàn bộ menu
        
        QUAN TRỌNG: 
        - Chỉ sử dụng thông tin từ menu thực tế được cung cấp
        - KHÔNG được bịa thêm thông tin món ăn, giá cả hoặc mô tả không có trong data
        - Đưa ra gợi ý cụ thể với ID món ăn, tên món, giá và mô tả chính xác từ menu
        - Luôn đề cập đến ID món ăn (itemId) khi giới thiệu món
        - Nếu không có thông tin trong menu, hãy nói rõ là không có thông tin
        
        Luôn đảm bảo thông tin chính xác và tư vấn nhiệt tình.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # 1. Check if user_input matches show_menu tool specifically
        tool = self._detect_show_menu_tool(user_input)
        if tool:
            # Extract parameters (simple: just return empty or all None, real use: NLP extract)
            parameters = {param: None for param in tool.get("parameters", [])}
            return self.create_response(
                action=tool["name"],
                parameters=parameters,
                natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} (service: {tool['service']})"
            )
        
        # 2. Lấy context từ knowledge base (menu items) và trả lời tự nhiên
        context = self._get_relevant_context(user_input)
        
        # Luôn đảm bảo có thông tin menu thực tế
        try:
            with open("data/menu_items.json", "r", encoding="utf-8") as f:
                menu_data = json.load(f)
            
            # Format menu data một cách rõ ràng với itemId
            menu_context = "MENU THỰC TẾ CỦA NHÀ HÀNG:\n" + "\n".join([
                f"🍽️ ID:{item['itemId']} - {item['name']}: {item['price']:,} VND - {item['description']}"
                for item in menu_data
            ])
            
            # Kết hợp context từ vector search với menu thực tế
            if context and context != "Không có thông tin bổ sung.":
                context = f"{menu_context}\n\nThông tin bổ sung:\n{context}"
            else:
                context = menu_context
                
        except Exception as e:
            context = "Không thể tải thông tin menu."
        
        prompt = f"""
        {self.get_system_prompt()}
        
        THÔNG TIN MENU THỰC TẾ CỦA NHÀ HÀNG:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        QUAN TRỌNG: Chỉ sử dụng thông tin từ menu thực tế được cung cấp ở trên. KHÔNG được bịa thêm thông tin món ăn, giá cả hoặc mô tả không có trong data.
        
        Hãy phân tích yêu cầu và trả lời chi tiết dựa trên menu thực tế:
        
        - Nếu hỏi về món đặc sản: Chỉ giới thiệu các món có trong menu thực tế, bao gồm ID món ăn
        - Nếu hỏi gợi ý món ăn: Đưa ra gợi ý cụ thể với ID món ăn, tên món, giá và mô tả từ menu thực tế
        - Nếu hỏi món theo giá: Phân loại theo tầm giá dựa trên giá thực tế trong menu, bao gồm ID món ăn
        - Nếu hỏi món theo loại: Phân loại món chính, đồ uống, tráng miệng dựa trên menu thực tế, bao gồm ID món ăn
        - Nếu hỏi món phổ biến: Chỉ liệt kê các món có trong menu thực tế, bao gồm ID món ăn
        
        Trả lời bằng tiếng Việt, thân thiện và chi tiết. CHỈ sử dụng thông tin từ menu được cung cấp. LUÔN bao gồm ID món ăn khi giới thiệu món.
        """
        
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Luôn sử dụng response_text trực tiếp thay vì cố gắng parse JSON
        # Vì AI có thể trả về text tự nhiên thay vì JSON
        return self.create_response(
            action="menu_suggestion",
            parameters={},
            natural_response=response_text
        )
    
    def _detect_show_menu_tool(self, user_input: str) -> Optional[Dict[str, Any]]:
        """
        Chỉ detect tool show_menu, bỏ qua các tools khác
        """
        if not self.tool_detector:
            return None
        
        # Chỉ tìm tool show_menu
        show_menu_tool = None
        for tool in self.tool_detector.tools:
            if tool.get("name") == "show_menu":
                show_menu_tool = tool
                break
        
        if not show_menu_tool:
            return None
        
        # Kiểm tra xem user input có match với show_menu không
        user_input_lower = user_input.lower()
        show_menu_keywords = ["xem menu", "cho xem menu", "hiển thị menu", "menu", "danh sách món"]
        
        for keyword in show_menu_keywords:
            if keyword in user_input_lower:
                return show_menu_tool
        
        return None
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format menu knowledge item
        """
        if isinstance(item, dict):
            # Format cho menu items từ JSON
            if "itemId" in item and "name" in item and "price" in item:
                price = item.get("price", 0)
                if isinstance(price, (int, float)):
                    price_str = f"{price:,} VND"
                else:
                    price_str = str(price)
                return f"🍽️ ID:{item.get('itemId', '')} - {item.get('name', '')} - {price_str} - {item.get('description', '')}"
            
            # Format cho các loại khác
            elif item.get("type") == "menu_item":
                return f"🍽️ {item.get('name', '')} - Giá: {item.get('price', '')} VND - {item.get('description', '')}"
            elif item.get("type") == "category":
                return f"📂 {item.get('name', '')} - {item.get('description', '')}"
            elif item.get("type") == "special":
                return f"⭐ {item.get('name', '')} - {item.get('description', '')}"
        
        return super()._format_knowledge_item(item)

    def handle_pending_action(self, user_input, tool, collected_params, missing_params, session_id, chat_session=None):
        # MenuAgent chỉ có show_menu, không có param bắt buộc nên luôn trả về show_menu
        return handle_pending_action_utils(
            user_input,
            tool,
            collected_params,
            missing_params,
            lambda u, t: {},  # Không cần extract param
            lambda p, t: [],  # Không có param thiếu
            lambda m, t, o: self.create_response(
                action="show_menu",
                parameters={},
                natural_response="Tôi sẽ thực hiện tác vụ: Hiển thị toàn bộ menu nhà hàng."
            ),
            self.create_response
        ) 