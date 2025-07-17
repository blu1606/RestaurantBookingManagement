import os
import json
import pickle
import tempfile
import hashlib
import logging
import threading
from collections import OrderedDict
from typing import Dict, Any, List, Optional
from langchain_google_genai import GoogleGenerativeAIEmbeddings

# Khởi tạo logger
logger = logging.getLogger("DataToolManager")
if not logger.hasHandlers():
    handler = logging.StreamHandler()
    formatter = logging.Formatter('[%(levelname)s] %(asctime)s %(name)s: %(message)s')
    handler.setFormatter(formatter)
    logger.addHandler(handler)
logger.setLevel(logging.INFO)

class LRUCache(OrderedDict):
    def __init__(self, maxsize=1000, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.maxsize = maxsize

    def __setitem__(self, key, value):
        if key in self:
            self.move_to_end(key)
        super().__setitem__(key, value)
        if len(self) > self.maxsize:
            oldest = next(iter(self))
            del self[oldest]

class DataToolManager:
    _instance = None
    _RAM_CACHE_SIZE = 1000
    _MAX_CACHE_SIZE_BYTES = 1 * 1024 * 1024 * 1024  # 1GB
    _MAX_BATCH_SIZE = 10  # Limit batch size to avoid timeouts

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(DataToolManager, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return
        self._data_cache = LRUCache(self._RAM_CACHE_SIZE)
        self._tools_cache = LRUCache(self._RAM_CACHE_SIZE)
        self._embeddings_cache = LRUCache(self._RAM_CACHE_SIZE)
        self._filtered_tools_cache = {}
        self._data_lock = threading.Lock()
        self._tools_lock = threading.Lock()
        self._embeddings_lock = threading.Lock()
        self.embedding_model = GoogleGenerativeAIEmbeddings(model="models/embedding-001")
        self._embedding_cache_dir = os.path.join(os.path.dirname(__file__), "embedding_cache")
        self._data_cache_dir = os.path.join(os.path.dirname(__file__), "data_cache")
        self._tools_cache_dir = os.path.join(os.path.dirname(__file__), "tools_cache")
        os.makedirs(self._embedding_cache_dir, exist_ok=True)
        os.makedirs(self._data_cache_dir, exist_ok=True)
        os.makedirs(self._tools_cache_dir, exist_ok=True)
        self._initialized = True
        # Dọn dẹp cache file nếu vượt quá giới hạn
        self.clear_old_cache_files()

    def _cleanup_cache_dir(self, cache_dir: str, max_size_bytes: int):
        files = [(os.path.join(cache_dir, f), os.path.getmtime(os.path.join(cache_dir, f)), os.path.getsize(os.path.join(cache_dir, f)))
                 for f in os.listdir(cache_dir) if os.path.isfile(os.path.join(cache_dir, f))]
        total_size = sum(size for _, _, size in files)
        if total_size <= max_size_bytes:
            return
        # Sắp xếp theo thời gian truy cập/sửa đổi tăng dần (cũ nhất trước)
        files.sort(key=lambda x: x[1])
        while total_size > max_size_bytes and files:
            file_path, _, size = files.pop(0)
            try:
                os.remove(file_path)
                logger.info(f"Removed old cache file: {file_path}")
                total_size -= size
            except Exception as e:
                logger.warning(f"Failed to remove cache file {file_path}: {e}")

    def clear_old_cache_files(self):
        self._cleanup_cache_dir(self._embedding_cache_dir, self._MAX_CACHE_SIZE_BYTES)
        self._cleanup_cache_dir(self._data_cache_dir, self._MAX_CACHE_SIZE_BYTES)
        self._cleanup_cache_dir(self._tools_cache_dir, self._MAX_CACHE_SIZE_BYTES)

    def _sha256(self, text: str) -> str:
        return hashlib.sha256(text.encode('utf-8')).hexdigest()

    def _embedding_cache_path(self, text: str) -> str:
        text_hash = self._sha256(text)
        return os.path.join(self._embedding_cache_dir, f"{text_hash}.pkl")

    def _data_cache_path(self, data_file: str) -> str:
        file_hash = self._sha256(data_file)
        return os.path.join(self._data_cache_dir, f"{file_hash}.pkl")

    def _tools_cache_path(self, tools_file: str) -> str:
        file_hash = self._sha256(tools_file)
        return os.path.join(self._tools_cache_dir, f"{file_hash}.pkl")

    def _safe_write_pickle(self, obj, path):
        try:
            with tempfile.NamedTemporaryFile('wb', delete=False, dir=os.path.dirname(path)) as tf:
                pickle.dump(obj, tf)
                tempname = tf.name
            os.replace(tempname, path)
        except Exception as e:
            logger.error(f"Error writing cache file {path}: {e}")

    def _safe_write_json(self, obj, path):
        try:
            with tempfile.NamedTemporaryFile('w', delete=False, dir=os.path.dirname(path), encoding='utf-8') as tf:
                json.dump(obj, tf, ensure_ascii=False)
                tempname = tf.name
            os.replace(tempname, path)
        except Exception as e:
            logger.error(f"Error writing JSON cache file {path}: {e}")

    def load_data(self, data_file: str, data_dir_candidates: List[str]) -> List[Dict[str, Any]]:
        with self._data_lock:
            if data_file in self._data_cache:
                logger.debug(f"RAM hit for data: {data_file}")
                return self._data_cache[data_file]
        cache_path = self._data_cache_path(data_file).replace('.pkl', '.json')
        file_path = None
        for data_dir in data_dir_candidates:
            candidate = os.path.join(data_dir, data_file)
            if os.path.exists(candidate):
                file_path = candidate
                break
        if file_path and os.path.exists(cache_path):
            try:
                cache_mtime = os.path.getmtime(cache_path)
                file_mtime = os.path.getmtime(file_path)
                if cache_mtime >= file_mtime:
                    with self._data_lock:
                        with open(cache_path, 'r', encoding='utf-8') as f:
                            data = json.load(f)
                            self._data_cache[data_file] = data
                            logger.info(f"File hit for data: {data_file}")
                            return data
            except Exception as e:
                logger.warning(f"Error loading JSON cache for data {data_file}: {e}, removing cache file.")
                try:
                    os.remove(cache_path)
                except Exception:
                    pass
        if file_path:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                with self._data_lock:
                    self._data_cache[data_file] = data
                try:
                    self._safe_write_json(data, cache_path)
                except Exception as e:
                    logger.error(f"Error writing JSON cache for data {data_file}: {e}")
                logger.info(f"Loaded data from source: {data_file}")
                return data
        with self._data_lock:
            self._data_cache[data_file] = []
        logger.info(f"Miss for data: {data_file}")
        return []

    def load_tools(self, tools_file: str) -> List[Dict[str, Any]]:
        with self._tools_lock:
            if tools_file in self._tools_cache:
                logger.debug(f"RAM hit for tools: {tools_file}")
                return self._tools_cache[tools_file]
        cache_path = self._tools_cache_path(tools_file).replace('.pkl', '.json')
        file_path = tools_file if os.path.exists(tools_file) else None
        if file_path and os.path.exists(cache_path):
            try:
                cache_mtime = os.path.getmtime(cache_path)
                file_mtime = os.path.getmtime(file_path)
                if cache_mtime >= file_mtime:
                    with self._tools_lock:
                        with open(cache_path, 'r', encoding='utf-8') as f:
                            tools = json.load(f)
                            self._tools_cache[tools_file] = tools
                            logger.info(f"File hit for tools: {tools_file}")
                            return tools
            except Exception as e:
                logger.warning(f"Error loading JSON cache for tools {tools_file}: {e}, removing cache file.")
                try:
                    os.remove(cache_path)
                except Exception:
                    pass
        if file_path:
            with open(file_path, 'r', encoding='utf-8') as f:
                tools = json.load(f)
                with self._tools_lock:
                    self._tools_cache[tools_file] = tools
                try:
                    self._safe_write_json(tools, cache_path)
                except Exception as e:
                    logger.error(f"Error writing JSON cache for tools {tools_file}: {e}")
                logger.info(f"Loaded tools from source: {tools_file}")
                return tools
        with self._tools_lock:
            self._tools_cache[tools_file] = []
        logger.info(f"Miss for tools: {tools_file}")
        return []

    def get_embedding(self, text: str) -> List[float]:
        with self._embeddings_lock:
            if text in self._embeddings_cache:
                logger.debug(f"RAM hit for embedding: {text[:30]}...")
                return self._embeddings_cache[text]
        cache_path = self._embedding_cache_path(text)
        if os.path.exists(cache_path):
            try:
                with self._embeddings_lock:
                    with open(cache_path, 'rb') as f:
                        embedding = pickle.load(f)
                        self._embeddings_cache[text] = embedding
                        logger.info(f"File hit for embedding: {text[:30]}...")
                        return embedding
            except Exception as e:
                logger.warning(f"Error loading cache for embedding: {e}, removing cache file.")
                try:
                    os.remove(cache_path)
                except Exception:
                    pass
        
        # Add retry logic for single embedding
        import time
        max_retries = 3
        retry_delay = 1  # seconds
        
        for attempt in range(max_retries):
            try:
                embedding = self.embedding_model.embed_query(text)
                with self._embeddings_lock:
                    self._embeddings_cache[text] = embedding
                try:
                    self._safe_write_pickle(embedding, cache_path)
                except Exception as e:
                    logger.error(f"Error writing cache for embedding: {e}")
                logger.info(f"Loaded embedding from source: {text[:30]}...")
                return embedding
            except Exception as e:
                if "504" in str(e) or "Deadline Exceeded" in str(e):
                    if attempt < max_retries - 1:
                        logger.warning(f"Embedding timeout (attempt {attempt + 1}/{max_retries}), retrying in {retry_delay}s...")
                        time.sleep(retry_delay)
                        retry_delay *= 2  # Exponential backoff
                        continue
                    else:
                        logger.error(f"Embedding failed after {max_retries} attempts: {e}")
                        return []  # Return empty embedding as fallback
                else:
                    logger.error(f"Embedding failed: {e}")
                    return []  # Return empty embedding as fallback
        
        return []  # Fallback

    def get_embeddings_batch(self, texts: List[str]) -> List[List[float]]:
        results = []
        uncached_indices = []
        uncached_texts = []
        # Kiểm tra cache trước
        for idx, text in enumerate(texts):
            with self._embeddings_lock:
                if text in self._embeddings_cache:
                    results.append(self._embeddings_cache[text])
                else:
                    results.append(None)
                    uncached_indices.append(idx)
                    uncached_texts.append(text)
        
        # Nếu có text chưa cache, gọi batch embedding nếu API hỗ trợ
        if uncached_texts:
            # Process in smaller batches to avoid timeouts
            all_batch_embeddings = []
            for i in range(0, len(uncached_texts), self._MAX_BATCH_SIZE):
                batch_texts = uncached_texts[i:i + self._MAX_BATCH_SIZE]
                batch_results = []
                
                # Thử gọi batch, nếu không có thì fallback từng cái
                embed_fn = getattr(self.embedding_model, 'embed_documents', None)
                if callable(embed_fn):
                    try:
                        # Thêm timeout và retry logic cho batch embedding
                        import time
                        max_retries = 3
                        retry_delay = 2  # seconds
                        
                        for attempt in range(max_retries):
                            try:
                                batch_results = self.embedding_model.embed_documents(batch_texts)
                                break  # Success, exit retry loop
                            except Exception as e:
                                if "504" in str(e) or "Deadline Exceeded" in str(e):
                                    if attempt < max_retries - 1:
                                        logger.warning(f"Batch embedding timeout (attempt {attempt + 1}/{max_retries}), retrying in {retry_delay}s...")
                                        time.sleep(retry_delay)
                                        retry_delay *= 2  # Exponential backoff
                                        continue
                                    else:
                                        logger.warning(f"Batch embedding failed after {max_retries} attempts, fallback to single: {e}")
                                        batch_results = []
                                        for t in batch_texts:
                                            try:
                                                emb = self.embedding_model.embed_query(t)
                                                batch_results.append(emb)
                                            except Exception as single_e:
                                                logger.error(f"Single embedding failed for text '{t[:30]}...': {single_e}")
                                                batch_results.append([])  # Empty embedding as fallback
                                        break
                                else:
                                    # Non-timeout error, fallback immediately
                                    logger.warning(f"Batch embedding failed, fallback to single: {e}")
                                    batch_results = []
                                    for t in batch_texts:
                                        try:
                                            emb = self.embedding_model.embed_query(t)
                                            batch_results.append(emb)
                                        except Exception as single_e:
                                            logger.error(f"Single embedding failed for text '{t[:30]}...': {single_e}")
                                            batch_results.append([])  # Empty embedding as fallback
                                    break
                    except Exception as e:
                        logger.warning(f"Batch embedding failed, fallback to single: {e}")
                        batch_results = []
                        for t in batch_texts:
                            try:
                                emb = self.embedding_model.embed_query(t)
                                batch_results.append(emb)
                            except Exception as single_e:
                                logger.error(f"Single embedding failed for text '{t[:30]}...': {single_e}")
                                batch_results.append([])  # Empty embedding as fallback
                else:
                    batch_results = []
                    for t in batch_texts:
                        try:
                            emb = self.embedding_model.embed_query(t)
                            batch_results.append(emb)
                        except Exception as single_e:
                            logger.error(f"Single embedding failed for text '{t[:30]}...': {single_e}")
                            batch_results.append([])  # Empty embedding as fallback
                
                all_batch_embeddings.extend(batch_results)
            
            # Cache kết quả
            for idx, emb in zip(uncached_indices, all_batch_embeddings):
                if emb:  # Only cache non-empty embeddings
                    with self._embeddings_lock:
                        self._embeddings_cache[texts[idx]] = emb
                    # Lưu ra file cache
                    cache_path = self._embedding_cache_path(texts[idx])
                    try:
                        self._safe_write_pickle(emb, cache_path)
                    except Exception as e:
                        logger.error(f"Error writing cache for embedding: {e}")
                results[idx] = emb
        
        return results

    def get_filtered_tools(self, tools_file: str, role: str, allowed_tools: list, role_permissions: list = None) -> list:
        # Cache key dựa trên file, role, allowed_tools, role_permissions
        cache_key = (tools_file, role, tuple(sorted(allowed_tools or [])), tuple(sorted(role_permissions or [])) if role_permissions else None)
        if cache_key in self._filtered_tools_cache:
            return self._filtered_tools_cache[cache_key]
        all_tools = self.load_tools(tools_file)
        filtered = []
        for tool in all_tools:
            if allowed_tools and tool.get("name") not in allowed_tools:
                continue
            if role_permissions and tool.get("name") not in role_permissions:
                continue
            filtered.append(tool)
        self._filtered_tools_cache[cache_key] = filtered
        return filtered

    def clear_cache(self):
        with self._data_lock:
            self._data_cache.clear()
        with self._tools_lock:
            self._tools_cache.clear()
        with self._embeddings_lock:
            self._embeddings_cache.clear() 