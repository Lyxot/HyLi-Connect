import requests
import json
import sys
import uuid
import time

ip_addr = sys.argv[1]
port = "15372"
url = "http://"+ip_addr+":"+port
uuid = 'ca0b8319-c30d-3880-e631-484c8b0cb8be'
nickname = 'test'
headers = {'content-type': 'application/json'}

def method_get(uri):
    response = requests.get(url+uri,headers=headers)
    print(uri, response.text)
    return json.loads(response.text)

def method_post(uri,data):
    response = requests.post(url+uri,data=data,headers=headers)
    print(uri, response.text)
    return json.loads(response.text)

def get_server_info():
    uri = "/api/info"
    response = method_get(uri)
    return json.dumps(response, indent=2)

def connect_to_server():
    uri = "/api/connect"
    response = method_post(uri,{"uuid":uuid,"nickname":nickname})
    return json.dumps(response, indent=2)

def get_client_list():
    uri = "/api/clients"
    response = method_post(uri,{"uuid":uuid})
    return json.dumps(response, indent=2)

def get_app_list():
    uri = "/api/apps"
    response = method_post(uri,{"uuid":uuid})
    return response


get_server_info()
connect_to_server()
get_client_list()
# get_app_list()