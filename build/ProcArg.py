import urllib.request
import json
import pickle
from Util import Properties
# from jsonpath_rw import jsonpath,parse
# python37
# input: properties path



if __name__ == "__main__":

    dictProperties=Properties("serverjs.properties").getProperties()
    # print(dictProperties["mqtthost"])
    restUri = "http://127.0.0.1:9123"
    # load semantics file
    api = "/mqtt/config/fromTrafficAnalysis"
    fpath = "../traffic_analysis/" + dictProperties['platformname'] + "/result.json"
    with open(fpath, 'r', encoding = 'utf8') as fp:
        data0 = fp.read()
        # data0 = json.load(fp) # result.json
        # print(data0)
    # data0 = ""  # result.json
    fpath = "../traffic_analysis/" + dictProperties['platformname'] + "/raw.json"
    with open(fpath, 'r', encoding = 'utf8') as fp:
        data1 = fp.read()
        # data1 = json.load(fp) # raw.json
        # print(data1)
    # data1 = ""  # raw.json
    # print("python: parsing arg")
    args = {}   # extra config, load from mqttjs.properties
    args["debug_result_yaml_save_path"] = dictProperties["debug_result_yaml_save_path"]  # platforms/mos_v3/dev1.yml
    args["userFilledTerms.host"] = dictProperties["mqtthost"]
    args["userFilledTerms.port"] = dictProperties["mqttport"]
    # pro = dictProperties["protocol"]
    if("protocol" in dictProperties):
        pro = dictProperties["protocol"]
        args["userFilledTerms.protocol"] = pro
        if 'mqtts' in pro:
            spro =  dictProperties["secureProtocol"]
            if(spro):
                args["secureProtocol"] = spro
    # key process to be updated
    # key1 = dictProperties["key1"]
    if("key1" in dictProperties):
        key1 = dictProperties["key1"]
        type_arg = "userFilledTerms."+dictProperties['platformname'] + '.password.key'
        args[type_arg] = key1
    
    # parsingOptions
    if(dictProperties['enforcePasswordUseRaw']):
        args["enforcePasswordUseRaw"] = True #
        args["parsingOptions.passwordRaw"] = dictProperties['passwordRaw']#
    # argsj = json.dumps(args)
    PostParamD = {}
    PostParamD["data0"] = data0
    PostParamD["data1"] = data1
    PostParamD["args"] = args
    PostParam = json.dumps(PostParamD)
    # print("yes " + PostParam)
    DATA = PostParam.encode('utf8')
    # print(restUri+api)
    req = urllib.request.Request(url = restUri+api, data=DATA, method='POST')
    # req.add_header('Content-type', 'application/x-www-form-urlencoded')
    r = urllib.request.urlopen(req).read()
    print("load semantics success")
    
    ### InitClient
    api = "/mqtt/config/initClient"
    req = urllib.request.Request(url = restUri+api, method='GET')
    r = urllib.request.urlopen(req).read()
    print("initClient result: " + r.decode('utf8'))

    ### Field Analysis
    api = "/mqtt/config/analyze/fields"
    req = urllib.request.Request(url = restUri+api, method='GET')
    r = urllib.request.urlopen(req).read()
    # print(r.decode('utf8'))
    
    ### store the result in json file
    data = json.loads(r.decode('utf-8'))
    # print(data)
    # fr = open(dictProperties['platformname']+'.pickle','wb')
    # pickle.dump(data,fr)
    filename = dictProperties['platformname']+'.json'
    fr2 = open(filename,'w')
    json.dump(data, fr2)
    # print(r1)
    # org_obj = json.loads(r.decode('utf8'))
    # print(org_obj['token'])
    fr2.close()
    
    ### load json file
    # data3=json.load(open(filename))
    # print(data3)

    
 
