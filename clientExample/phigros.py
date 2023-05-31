from requests import Session

s = Session()

sessionToken = "h0kg9ba19ulrlz408o22g44pq"
result = s.get("http://localhost:9090/saveUrl/%s" % sessionToken)
json = result.json()
print(json)
print(json["saveUrl"])

url = "http://localhost:9090/b19/%s" % json["saveUrl"]
print(url)
result = s.get(url)
print(result.json())