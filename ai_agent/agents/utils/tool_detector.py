import os
import json
from typing import Dict, Any, List, Optional
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from sklearn.metrics.pairwise import cosine_similarity


class ToolDetector:
    """
    Tool detector sử dụng embeddings và vector similarity để cải thiện độ chính xác
    Singleton pattern để tránh khởi tạo nhiều lần
    """
    
    _instance = None
    _initialized = False
    
    def __new__(cls, tools: List[Dict[str, Any]] = None, embedding_model_name: str = "models/embedding-001"):
        if cls._instance is None:
            cls._instance = super(ToolDetector, cls).__new__(cls)
        return cls._instance
    
    def __init__(self, tools: List[Dict[str, Any]] = None, embedding_model_name: str = "models/embedding-001"):
        # Chỉ khởi tạo một lần
        if ToolDetector._initialized:
            return
            
        self.tools = tools or []
        try:
            # Sử dụng GoogleGenerativeAIEmbeddings cho embeddings
            self.embedding_model = GoogleGenerativeAIEmbeddings(model=embedding_model_name)
            self.tool_embeddings = self._precompute_tool_embeddings()
            print(f"✅ ToolDetector: Initialized with {len(self.tools)} tools")
            ToolDetector._initialized = True
        except Exception as e:
            print(f"🔥 ToolDetector: Error initializing embedding model: {e}")
            self.embedding_model = None
            self.tool_embeddings = {}
    
    @classmethod
    def get_instance(cls, tools: List[Dict[str, Any]] = None) -> 'ToolDetector':
        """
        Lấy instance singleton của ToolDetector
        """
        if cls._instance is None:
            cls._instance = cls(tools)
        elif tools and cls._instance.tools != tools:
            # Nếu tools thay đổi, refresh embeddings
            cls._instance.tools = tools
            ToolDetector._initialized = False  # Cho phép re-init embeddings
            cls._instance.__init__(tools)
        return cls._instance
    
    def _get_embedding(self, text: str) -> List[float]:
        """
        Lấy embedding cho text sử dụng GoogleGenerativeAIEmbeddings
        """
        try:
            if not self.embedding_model:
                return []
            
            # Add retry logic for embedding
            import time
            max_retries = 3
            retry_delay = 1  # seconds
            
            for attempt in range(max_retries):
                try:
                    return self.embedding_model.embed_query(text)
                except Exception as e:
                    if "504" in str(e) or "Deadline Exceeded" in str(e):
                        if attempt < max_retries - 1:
                            print(f"⚠️ ToolDetector: Embedding timeout (attempt {attempt + 1}/{max_retries}), retrying in {retry_delay}s...")
                            time.sleep(retry_delay)
                            retry_delay *= 2  # Exponential backoff
                            continue
                        else:
                            print(f"🔥 ToolDetector: Embedding failed after {max_retries} attempts: {e}")
                            return []
                    else:
                        print(f"🔥 ToolDetector: Error getting embedding: {e}")
                        return []
            
            return []
        except Exception as e:
            print(f"🔥 ToolDetector: Error getting embedding: {e}")
            return []
    
    def _precompute_tool_embeddings(self) -> Dict[str, List[float]]:
        """
        Tính toán và lưu trữ embeddings cho mỗi tool
        """
        tool_embeddings_map = {}
        for tool in self.tools:
            tool_name = tool.get("name", "unknown_tool")
            description = tool.get("description", "")
            example = tool.get("example_user_prompt", "")
            
            # Kết hợp description và example để tạo một embedding tổng thể
            combined_text = f"{description}. {example}".strip()
            if combined_text:
                embedding = self._get_embedding(combined_text)
                if embedding:
                    tool_embeddings_map[tool_name] = embedding
                else:
                    print(f"⚠️ ToolDetector: Failed to get embedding for tool {tool_name}")
            else:
                print(f"⚠️ ToolDetector: No text content for tool {tool_name}")
        
        print(f"✅ ToolDetector: Precomputed embeddings for {len(tool_embeddings_map)} tools")
        return tool_embeddings_map
    
    def detect_tool_from_prompt(self, user_input: str, agent_context: str = None) -> Optional[Dict[str, Any]]:
        """
        Detect if user_input matches any tool using semantic similarity
        Returns the best matching tool and its confidence score
        """
        if not user_input.strip() or not self.embedding_model:
            return None
        
        user_embedding = self._get_embedding(user_input)
        if not user_embedding:
            return None
        
        best_match = None
        best_score = 0.0
        
        for tool in self.tools:
            tool_name = tool.get("name", "unknown_tool")
            tool_embed = self.tool_embeddings.get(tool_name)
            
            if tool_embed:
                # Tính toán cosine similarity
                try:
                    score = cosine_similarity([user_embedding], [tool_embed])[0][0]
                    
                    # Thêm boost nhỏ cho khớp tên chính xác
                    if tool_name.lower() in user_input.lower():
                        score += 0.1
                    
                    # Ưu tiên create_booking khi không có bookingId
                    if "booking" in tool_name.lower():
                        if "create_booking" in tool_name.lower():
                            # Nếu không có số ID trong input, ưu tiên create
                            if not any(char.isdigit() for char in user_input):
                                score += 0.2
                        elif "update_booking" in tool_name.lower():
                            # Nếu có số ID trong input, ưu tiên update
                            if any(char.isdigit() for char in user_input):
                                score += 0.2
                    
                    # Thêm boost nhỏ cho các từ khóa chung
                    user_input_lower = user_input.lower()
                    if "xem" in user_input_lower or "hiển thị" in user_input_lower or "cho xem" in user_input_lower:
                        if "show" in tool_name.lower() or "hiển thị" in tool.get("description", "").lower():
                            score += 0.1
                    elif "thêm" in user_input_lower or "tạo" in user_input_lower:
                        if "add" in tool_name.lower() or "thêm" in tool.get("description", "").lower():
                            score += 0.1
                    elif "xóa" in user_input_lower:
                        if "delete" in tool_name.lower() or "xóa" in tool.get("description", "").lower():
                            score += 0.1
                    elif "đặt bàn" in user_input_lower or "đặt" in user_input_lower:
                        if "create_booking" in tool_name.lower():
                            score += 0.3  # Boost cao hơn cho create_booking
                        elif "update_booking" in tool_name.lower():
                            # Chỉ boost update_booking nếu có từ khóa "cập nhật", "thay đổi", "sửa"
                            if any(word in user_input_lower for word in ["cập nhật", "thay đổi", "sửa", "update"]):
                                score += 0.2
                    elif "hủy" in user_input_lower or "cancel" in user_input_lower:
                        if "cancel" in tool_name.lower() or "hủy" in tool.get("description", "").lower():
                            score += 0.15
                    elif "hủy" in user_input_lower or "cancel" in user_input_lower:
                        if "cancel" in tool_name.lower() or "hủy" in tool.get("description", "").lower():
                            score += 0.15
                    
                    if score > best_score:
                        best_score = score
                        best_match = tool
                        
                except Exception as e:
                    print(f"🔥 ToolDetector: Error calculating similarity for {tool_name}: {e}")
                    continue
        
        # Giảm ngưỡng confidence để dễ detect hơn
        CONFIDENCE_THRESHOLD = 0.6  # Giảm từ 0.75 xuống 0.6
        if best_match and best_score >= CONFIDENCE_THRESHOLD:
            best_match["confidence_score"] = float(f"{best_score:.3f}")
            print(f"🎯 ToolDetector: Matched '{user_input}' to '{best_match['name']}' (confidence: {best_score:.3f})")
            return best_match
        
        print(f"⚠️ ToolDetector: No confident match for '{user_input}' (best score: {best_score:.3f})")
        return None
    
    def refresh_embeddings(self):
        """
        Refresh embeddings khi tools thay đổi
        """
        if self.tools:
            self.tool_embeddings = self._precompute_tool_embeddings()
            print(f"✅ ToolDetector: Refreshed embeddings for {len(self.tools)} tools")
    
    def get_tool_by_name(self, tool_name: str) -> Optional[Dict[str, Any]]:
        """
        Lấy tool theo tên
        """
        for tool in self.tools:
            if tool.get("name") == tool_name:
                return tool
        return None
    
    def get_all_tools(self) -> List[Dict[str, Any]]:
        """
        Lấy danh sách tất cả tools
        """
        return self.tools.copy()
    
    def get_tools_by_service(self, service_name: str) -> List[Dict[str, Any]]:
        """
        Lấy danh sách tools theo service
        """
        return [tool for tool in self.tools if tool.get("service") == service_name]
    
    def suggest_tool_with_gemini(self, user_input: str, tools: Optional[List[Dict[str, Any]]] = None, model_name: str = "gemini-1.5-flash") -> Optional[str]:
        """
        Sử dụng Gemini LLM để suggest tool phù hợp nhất cho user_input.
        Trả về tên tool (str) hoặc None nếu không xác định được.
        """
        import google.generativeai as genai
        import os
        try:
            genai.configure(api_key=os.getenv("GOOGLE_API_KEY"))
            model = genai.GenerativeModel(model_name)
        except Exception as e:
            print(f"🔥 ToolDetector: Error configuring Gemini: {e}")
            return None

        tools = tools or self.tools
        if not tools or not user_input.strip():
            return None

        # Xây dựng prompt
        prompt_parts = [
            "Bạn là AI chuyên chọn tool phù hợp nhất để xử lý yêu cầu của người dùng.",
            "Dưới đây là danh sách các tool, mô tả và ví dụ:",
            "---"
        ]
        for tool in tools:
            prompt_parts.append(f"Tool: {tool.get('name','')}")
            prompt_parts.append(f"Mô tả: {tool.get('description','')}")
            example = tool.get('example_user_prompt') or tool.get('example', '')
            if example:
                prompt_parts.append(f"Ví dụ: {example}")
            prompt_parts.append("---")
        prompt_parts.extend([
            "Nhiệm vụ của bạn là chọn tool phù hợp nhất cho yêu cầu sau.",
            "QUY TẮC:",
            "1. Chỉ trả về tên tool (ví dụ: create_booking, show_menu, ...)",
            "2. Không thêm ký tự, dấu câu, hoặc văn bản khác.",
            "3. Nếu không có tool nào phù hợp, trả về NONE.",
            "",
            f"User Query: {user_input}",
            "",
            "Tool:"
        ])
        prompt = "\n".join(prompt_parts)

        try:
            response = model.generate_content(prompt)
            tool_name = response.text.strip()
            # Chuẩn hóa output
            tool_name = tool_name.replace('"', '').replace("'", '').replace(".", '').strip()
            if tool_name.upper() == "NONE":
                return None
            # Kiểm tra tool có trong danh sách không
            for tool in tools:
                if tool_name == tool.get('name'):
                    return tool_name
            print(f"⚠️ ToolDetector: Gemini trả về tool không hợp lệ: {tool_name}")
            return None
        except Exception as e:
            print(f"🔥 ToolDetector: Error calling Gemini for tool suggestion: {e}")
            return None 