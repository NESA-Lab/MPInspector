import os,re,copy,pickle
from bs4 import BeautifulSoup

def storefile(file_name,content):
    f = open(file_name,'wb')
    f.write(content)
    f.close()

def iscode_has_codelang(tag):
    if 'code' in tag.name:
        if tag.has_attr('class'):
            classvalues = ','.join(tag['class'])
            if 'lang-' in classvalues:
                # print(tag['class'])
                return True
    return False

# extract the html file
# htmlname = "other platforms/webnms data/WebNMS IoT - Developer Guide.html"

def preprocess(htmlname):
    # htmlname = "other platforms/ptc thingworx/PTC Help Center.html"
    soup=BeautifulSoup(open(htmlname,'r', errors="ignore"),features='html.parser')
    tablelist = [] # here store the table data, tablelist[0] = ['<td>iothubowner</td>', '<td>All permission</td>']  it might be several column
    # soup.find_all("a",attrs={"id":"link2"})
    ## delete code block, example: <code class="lang-javascript">   
    for langcode in soup.find_all(iscode_has_codelang): 
        # print(div)
        langcode.decompose()
    ## delete script
    for script in soup.find_all('script'): 
        # print(div)
        script.decompose()
    # remain code
    for code in soup.find_all('code'):
        print(code.string)
        if code.string:
            tmp = '<code>'+ code.string+'</code>'
            code.string = tmp
            print(code.string)

    # handle table ## table has table tag
    for table_node in soup.find_all('table'):
        td_tmp = []
        for tr in table_node.find_all('tr'):
            td = tr.find_all('td')
            item_tmp = []
            for item in td:
                item_string = ""
                for string in item.stripped_strings:
                    # DEBUG
                    # print("******")
                    string_1 = string.replace('\n','$nn')
                    # print(string_1)
                    string_2 = ' '.join(string_1.split())
                    newstring = string_2.replace('$nn','\n')
                    # print(' '.join(string.split()))
                    item_string = item_string+ newstring
                item_tmp.append(item_string)
            if len(item_tmp) !=0:
                tablelist.append(item_tmp)
            # td_tmp = copy.deepcopy(td_contents)
            # if len(td_tmp)!= 0:
            #     tablelist.append(td_tmp)
            # tablelist = copy.deepcopy(tmp)
        table_node.decompose()
    # refine table list
    # for item in tablelist:
    #     if len(item) == 0:
    #         tablelist.remove(item)
    # DEBUG
    print(tablelist)
    # output the text
    content = soup.text
    # print(type(content))
    # print(content.strip().encode('gbk'))
    resultname = htmlname.split('.html')[0]
    storefile(resultname+'result.txt', bytes(content.strip(), encoding = 'gbk',errors="ignore"))
    # use dump() to store the table data
    # the table data is a list, the list stores lists of column data
    # example: [[a,b,c],[a,b,c]] 
    fw = open(resultname+'table.txt','wb')  
    # Pickle the list using the highest protocol available.  
    # pickle.dump(tablelist, fw, -1)  
    # Pickle dictionary using protocol 0.  
    pickle.dump(tablelist, fw)  
    fw.close()
    return resultname



def main():
    tmp = preprocess("other platforms/ptc thingworx/PTC Help Center.html")
    print(tmp)

if __name__ == '__main__':
    main()
