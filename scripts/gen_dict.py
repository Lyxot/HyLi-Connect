# 混淆字典生成
import random
import sys
length = 10
result = set()

keys = ["l","I","1"]

for o in range(1,100000):
    # 长度 5-11 位
    for length in range(4,11):
        # 按照长度随机拼接
        temp = keys[random.randint(0,1)]
        for i in range(1, length+1):
            temp += random.choice(keys)
        result.add(temp)


print(f"成功生成字典，数量：", len(result))

if len(sys.argv) > 1:
    path = sys.argv[1] + "/dict.txt"
else:
    path = "dict.txt"

with open(path,mode='w+',encoding='utf-8') as f:
    f.writelines("\n".join(result))
    f.flush()
