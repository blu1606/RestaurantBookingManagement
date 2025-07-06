import os
import json
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_google_genai import GoogleGenerativeAIEmbeddings
import google.generativeai as genai


from ..utils.rate_limiter import RateLimiter


class BaseAgent(ABC):
    """
    Base class cho tất cả AI Agent chuyên biệt
    """
    
    def __init__(self, agent_name: str, data_files: list = None, gemini_model=None):
        self.agent_name = agent_name
        self.data_files = data_files or []
        self.gemini_model = gemini_model
        self.vector_db = None
        self.retriever = None
        self.knowledge_base = []
        self.rate_limiter = RateLimiter(max_requests=8, time_window=60)  # Conservative rate limiting
        
        # Load knowledge base from data files
        self._load_knowledge_base()
    
    def _load_knowledge_base(self):
        """
        Load knowledge base từ các file data JSON
        """
        try:
            self.knowledge_base = []
            data_dir = "../../data"  # Path to data folder
            
            # Load from specified data files
            for data_file in self.data_files:
                file_path = os.path.join(data_dir, data_file)
                if os.path.exists(file_path):
                    with open(file_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        # Add metadata to identify source
                        for item in data:
                            item['_source_file'] = data_file
                        self.knowledge_base.extend(data)
                    print(f"✅ {self.agent_name}: Loaded data from {data_file}")
                else:
                    print(f"⚠️ {self.agent_name}: Data file {data_file} not found at {file_path}")
            
            if self.knowledge_base:
                print(f"✅ {self.agent_name}: Loaded {len(self.knowledge_base)} items from data files")
                # Build vector database
                self._build_vector_db()
            else:
                print(f"⚠️ {self.agent_name}: No data files loaded")
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error loading knowledge base: {e}")
    
    def _build_vector_db(self):
        """
        Xây dựng vector database từ knowledge base
        """
        try:
            if not self.knowledge_base:
                return
            
            # Convert knowledge base to documents
            documents = []
            for item in self.knowledge_base:
                content = self._format_knowledge_item(item)
                metadata = {"source": self.agent_name, "type": item.get("type", "general")}
                documents.append(Document(page_content=content, metadata=metadata))
            
            if documents:
                embeddings = GoogleGenerativeAIEmbeddings(model="models/embedding-001")
                self.vector_db = Chroma.from_documents(documents, embeddings)
                self.retriever = self.vector_db.as_retriever(search_kwargs={"k": 3})
                print(f"✅ {self.agent_name}: Vector DB built with {len(documents)} documents")
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error building vector DB: {e}")
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format knowledge item thành text (có thể override trong subclass)
        """
        if isinstance(item, dict):
            return json.dumps(item, ensure_ascii=False)
        return str(item)
    
    def _get_relevant_context(self, user_input: str) -> str:
        """
        Lấy context liên quan từ vector database
        """
        if not self.retriever:
            return "Không có thông tin bổ sung."
        
        try:
            relevant_docs = self.retriever.invoke(user_input)
            context = "\n".join([doc.page_content for doc in relevant_docs])
            return context if context else "Không có thông tin bổ sung."
        except Exception as e:
            print(f"⚠️ {self.agent_name}: Error retrieving context: {e}")
            return "Không có thông tin bổ sung."
    
    def _call_gemini(self, prompt: str, chat_session: Optional[Any] = None) -> str:
        """
        Gọi Gemini API, có thể sử dụng chat_session để duy trì ngữ cảnh
        """
        if not self.gemini_model:
            return "Xin lỗi, AI service hiện không khả dụng."
        
        # Apply rate limiting
        self.rate_limiter.wait_if_needed()
        
        try:
            if chat_session:
                # Nếu có chat_session, gửi message vào session
                response = chat_session.send_message(prompt)
            else:
                # Nếu không, tạo một phiên độc lập
                response = self.gemini_model.generate_content(prompt)
            return response.text
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error calling Gemini: {e}")
            return "Xin lỗi, có lỗi xảy ra khi xử lý yêu cầu."
    
    def refresh_knowledge(self):
        """
        Refresh knowledge base từ file
        """
        self._load_knowledge_base()
    
    @abstractmethod
    def get_system_prompt(self) -> str:
        """
        Trả về system prompt cho agent này
        """
        pass
    
    @abstractmethod
    def process_request(self, user_input: str, session_id: str = "default", chat_session: Optional[Any] = None) -> Dict[str, Any]:
        """
        Xử lý yêu cầu của user
        """
        pass
    
    def create_response(self, action: str, parameters: Dict[str, Any], 
                       natural_response: str) -> Dict[str, Any]:
        """
        Tạo response chuẩn
        """
        return {
            "action": action,
            "parameters": parameters,
            "naturalResponse": natural_response,
            "agent": self.agent_name
        }
    
    def _parse_json_response(self, response_text: str, fallback_action: str = "clarify", 
                           fallback_response: str = "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại.") -> Dict[str, Any]:
        """
        Parse JSON response from Gemini with improved error handling
        """
        try:
            # First, try to parse as-is
            return json.loads(response_text)
        except json.JSONDecodeError:
            # Try to extract JSON from the response using regex
            import re
            json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
            if json_match:
                try:
                    return json.loads(json_match.group())
                except json.JSONDecodeError:
                    pass
            
            # If all parsing attempts fail, return fallback
            print(f"⚠️ {self.agent_name}: Failed to parse JSON response: {response_text[:100]}...")
            return {
                "action": fallback_action,
                "parameters": {},
                "naturalResponse": fallback_response
            } 