from requests import Session

s = Session()
result = s.get("http://localhost:9090/saveUrl/h0kg9ba19ulrlz408o22g44pq")
json = result.json()
print(json)
print(json["saveUrl"])

url = "http://localhost:9090/b19/%s" % json["saveUrl"]
print(url)
result = s.get(url)
print(result.json())