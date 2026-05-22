import json
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
import dashscope
from dashscope import TextEmbedding

# --- 配置区域 ---
# 🚨 请填入你的阿里云 DashScope API Key
dashscope.api_key = "sk-29d9a46a7864473496526ee10db1d79e"

# --- 1. 连接 Qdrant 并准备读取数据 ---
client = QdrantClient("http://localhost:6333")
collection_name = "writing_exemplars"

print("正在读取 JSON 文件...")
with open("qdrant/ielts_data_export.json", "r", encoding="utf-8") as f:
    records = json.load(f)

if not records:
    print("JSON 文件是空的！")
    exit()

# --- 2. 使用阿里云 API 获取真实的 Embedding ---
print("正在调用阿里云 API 生成向量，请稍候...")

# 我们提取 essayBody（雅思范文正文）作为用来被检索的核心向量内容
texts = [r["essayBody"] for r in records]

# 阿里云 API 建议分批请求（这里设置为每次处理 10 条）
batch_size = 10
all_embeddings = []

for i in range(0, len(texts), batch_size):
    batch_texts = texts[i:i+batch_size]
    print(f"正在请求 API: 处理第 {i+1} 到 {min(i+batch_size, len(texts))} 条数据...")

    # 调用阿里云 text-embedding-v4 模型
    resp = TextEmbedding.call(
        model="text-embedding-v4",
        input=batch_texts
    )

    if resp.status_code == 200:
        # 提取并保存生成的向量
        for emb in resp.output['embeddings']:
            all_embeddings.append(emb['embedding'])
    else:
        print(f"❌ API 调用失败！错误信息: {resp.message}")
        exit()

# 阿里云的 text-embedding-v2 模型输出的真实维度是 1536 维
vector_size = len(all_embeddings[0])
print(f"✅ 向量生成完毕！真实的向量维度为: {vector_size}")

# --- 3. 彻底清空旧数据并重建集合 ---
# 这一步会自动炸掉你之前所有的“占位向量”空壳数据，并用 1536 维重新建表
print(f"正在清空旧数据，并按 {vector_size} 维重建集合 {collection_name}...")
client.recreate_collection(
    collection_name=collection_name,
    vectors_config=VectorParams(size=vector_size, distance=Distance.DOT),
)

# --- 4. 组装全新 Payload 并灌入数据 ---
print("正在组装新字段并灌入 Qdrant...")
points = []
for idx, r in enumerate(records):

    # 严格按照你的要求重命名字段
    payload = {
        "exemplar_id": r["topicId"],
        "excerpt": r["essayBody"],
        "task_type": r["taskType"],
        "category": r["categoryName"],
        "band_score": r["bandScore"],
        "topic_name": r["topicName"],
        "question_text": r["questionText"],
    }

    points.append(
        PointStruct(
            id=r["topicId"],  # 保持 ID 不变
            vector=all_embeddings[idx], # 填入阿里云生成的真实向量
            payload=payload
        )
    )

# 批量高并发写入 Qdrant
client.upload_points(collection_name=collection_name, points=points)

print(f"🎉 完美搞定！成功将 {len(points)} 条带真实阿里云向量的数据导入本地 Qdrant。")