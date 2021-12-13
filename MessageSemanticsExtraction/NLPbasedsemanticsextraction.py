import os 
os.environ['CUDA_VISIBLE_DEVICES'] = '1'
import pickle,re
from flair.data import Sentence
from flair.models import SequenceTagger
from flair.models import MultiTagger
from stanfordcorenlp import StanfordCoreNLP
from webpageprocess import preprocess
from Util import Properties
import torch
import json
import copy
import argparse

# print(torch.cuda.is_available())
# use type() to infer the objection

# Function: check if the sentence contains keyWord
# input: sentence, keyWord
# output: Ture or False
# def checkSentenceContainKeywords(sentence, keyWord):
#     nlp1 = StanfordCoreNLP(r'/home/standard-corenlp-full-2021-01-07/')
#         # print(nlp.word_tokenize(sentence))
#     WordsinSentence = nlp1.word_tokenize(sentence)
#     nlp1.close()
#     for word in WordsinSentence:
#         if isSameMeaning(word, keyWord):
#             return True
#     return False


# Function: split paragraphIntoSentences
# input: string
# output: a list of sentences
def splitParagraphIntoSentences(paragraph):
  import re
  sentenceEnders = re.compile(r"""
      # Split sentences on whitespace between them.
      (?:               # Group for two positive lookbehinds.
        (?<=[.!?])      # Either an end of sentence punct,
      | (?<=[.!?]['"])  # or end of sentence punct and quote.
      )                 # End group of two positive lookbehinds.
      (?<!  Mr\.   )    # Don't end sentence on "Mr."
      (?<!  Mrs\.  )    # Don't end sentence on "Mrs."
      (?<!  Jr\.   )    # Don't end sentence on "Jr."
      (?<!  Dr\.   )    # Don't end sentence on "Dr."
      (?<!  Prof\. )    # Don't end sentence on "Prof."
      (?<!  Sr\.   )    # Don't end sentence on "Sr."."
    \s+               # Split on whitespace between sentences.
    """, 
    re.IGNORECASE | re.VERBOSE)
  sentenceList = sentenceEnders.split(paragraph)
  return sentenceList

# Function: Find all the sentences in the specified document that contain the keyword
# Input: documentPath & keyWord[Case Insensitive]
# Output: A list of sentences that contain the key word
def findSentencesContainKeyWords(documentPath,keyWord):
    if len(keyWord) == 0:
        return []
    words = [] # all the words in the document
    codeflag = 0 
    with open(documentPath, 'r', encoding='utf-8', errors='ignore') as f:
        sentences = f.readlines()
        # Use space or '\n' to split the document into words
        for sentence in sentences:
            if len(sentence)==0:
                continue
            if '<code>' in sentence:
                codeflag = 1
            if '</code>' in sentence:
                codeflag = 0
            linewords = sentence.split()
            if codeflag == 1:
                linewords.append('\n')
            words.extend(linewords)
        # DEBUG
        # print(words)
    # Find the sentences that contain the key words
    sentences = [] # all the sentences that contain the key words
    keyWord = keyWord.split() # keyWord may contain ' ', eg. Client Id
    i = 0
    while i>=0 and i<len(words):    
        sentence = ''
        flag = 1
        for j in range(0,len(keyWord)):
            if i+j>=len(words) or (keyWord[j].lower() not in words[i+j].lower() and not isSameMeaning(keyWord[j],words[i+j])):
                flag = 0
                break
            else:
                sentence = sentence + ' ' + words[i+j]
        if flag == 1:
            # search forwards
            start = i-1
            while start>=0 and '.' != words[start][-1]:
                sentence = words[start]+ ' ' + sentence
                start-=1    
            # search backwards
            end = i+len(keyWord)
            
            if '.' != words[i+len(keyWord)-1][-1]: 
                while end<len(words) and '.' != words[end][-1]:
                    sentence = sentence + ' ' + words[end]
                    end+=1
                if end<len(words) and '.' == words[end][-1]:
                    sentence = sentence + ' ' + words[end]
                    i = end
            i = end - 1
            sentence = sentence.strip()
            sentences.append(sentence)
        i+=1
    # DEBUG
    # print(keyWord)
    # print(":")
    # print(sentences)
    return sentences



# function: get the item of the table data
# input: table filename, keyWord
# output: a list that contain columns that contains keyWord
def findTabledatacontainKeywords(filename, keyWord):
    try:
        rawtabledata = loadpicklefile(filename)
    except IOError:
        print('Error:cannot find'+filename)
        return []
    tabledata = []
    keyWords = keyWord.split()
    for i in range(len(rawtabledata)):
        ContainKeywordFlag = False
        for j in range(len(rawtabledata[i])):
            if keyWord.lower() in rawtabledata[i][j].lower(): # test if the keyword is in rawdata[i][j]
                ContainKeywordFlag = True
                break
        if ContainKeywordFlag:
            tabledata.append(rawtabledata[i])  
    print(tabledata)
    return tabledata

def changeJsonDictToList(jsondict):
    items = []
    # 
    for key in jsondict:
        items.append({'key':delSpecialCharacter(key),'items':[jsondict[key]]})
    return items

# Function: get the items of the structured keywords
# Input: keyWord and the sentence segment related to the keyWord
# Output: a dict contains two fields: key and items[a list of items]
# eg. input:mqttUsername: deviceName+"&"+productKey then output will be {'key':'mqttUsername','items':['deviceName','productKey']}
# eg. input:smqttPassword: sign_hmac(deviceSecret,content)sign then output will be {'key':'mqttPassword','items':[{'key':'sign_hmac',items:['deviceSecret','content']}]}
def processStructuredKeyWord(keyWord,sentence):
    # A = B/C/D=E BC cannot find their definition
    if sentence.find(keyWord)!=-1:
        idx = sentence.find(keyWord)+len(keyWord)
        while idx<len(sentence):
            if sentence[idx]==':' or sentence[idx]=='=':
                # try json first
                try:
                    jsondict = json.loads(sentence[idx+1:].strip())
                    items = changeJsonDictToList(jsondict)
                    return {'key':delSpecialCharacter(keyWord),'items':items}
                except:
                    break   
            if sentence[idx]==',' or sentence[idx]=='+' or sentence[idx]=='/' or sentence[idx]=='&':
                return {'key':keyWord,'items':[]}
            idx+=1
    res = processStructuredKeyWordInner(keyWord,sentence,0)
    return {'key':delSpecialCharacter(res['key']),'items':res['items']}

def delSpecialCharacter(word):
    idx = 0
    word = word.strip()
    word = word.replace('<code>','')
    if len(word.split())>2: 
        return ''
    while idx<len(word):
        if word[idx] == '|' or word[idx] == '{' or word[idx] == '}' or word[idx] == '"' or word[idx] == '\'' or word[idx] == '$':
            if idx-1>=0 and idx+1<len(word):
                word = word[0:idx]+word[idx+1:]
                idx-=1
            elif idx-1<0:
                word = word[1:]
                idx-=1
            elif idx+1>=len(word):
                word = word[0:-1]
                break
        idx+=1
    return word
def processStructuredKeyWordInner(keyWord,sentence,inIdx):
    items = []
    idx = inIdx
    isKeyWordEnd = False
    curItem = ''
    bracketFlag = False
     
    while idx<len(sentence):
        if (not isKeyWordEnd) and (sentence[idx]==':' or sentence[idx]=='=' or sentence[idx]=='('):
            if sentence[idx]=='(':
                bracketFlag = True
            isKeyWordEnd = True
            idx+=1
            continue   
        if not isKeyWordEnd:
            idx+=1
            continue
        if isKeyWordEnd and sentence[idx]!='=' and sentence[idx]!=':' and sentence[idx]!='+' and sentence[idx]!=',' and sentence[idx]!='/'  and sentence[idx] !='|' and sentence[idx]!='(' and sentence[idx]!=')' and (sentence[idx]!='&'or (sentence[idx]=='&' and (sentence[idx-1]=='"' or sentence[idx-1]=='\''))):
            curItem+=sentence[idx]
            idx+=1
            continue
        if isKeyWordEnd and (sentence[idx]=='+' or sentence[idx]=='&' or sentence[idx]==',' or sentence[idx]=='/' or sentence[idx]=='|'):
            if bracketFlag:
                items.append(delSpecialCharacter(curItem))
                curItem = ''
                idx+=1
                continue
            else:
                items.append(delSpecialCharacter(curItem))
                curItem = ''
                if inIdx == 0 and sentence[idx]!=',':
                    idx+=1
                    continue
                else:
                    return {'key':delSpecialCharacter(keyWord),'items':items,'endIdx':idx}
        if isKeyWordEnd and (sentence[idx]=='(' or sentence[idx]=='=' or sentence[idx]==':'):
            curItem = processStructuredKeyWordInner(curItem,sentence,idx-len(curItem))
            idx = curItem['endIdx']+1
            items.append({'key':delSpecialCharacter(curItem['key']),'items':curItem['items']})
            curItem = ''
            continue
        if isKeyWordEnd and sentence[idx]==')':
            items.append(delSpecialCharacter(curItem))
            return {'key':delSpecialCharacter(keyWord),'items':items,'endIdx':idx}
    if curItem != '':
        items.append(delSpecialCharacter(curItem))
    return {'key':delSpecialCharacter(keyWord),'items':items,'endIdx':idx}

# Function: get next word
def getNextWord(sentence,beginRange):
    i = beginRange
    curWord = ''
    while i<len(sentence) and sentence[i]!=' ' and sentence[i]!='=' and sentence[i]!=':' and sentence[i]!=';':
        curWord+=sentence[i]
        i+=1
    return {'curWord':curWord,'beginRange':i+1}
# Function: get the keyWord end idx of the sentence
def getKeyWordRangeIdx(sentence,keyWord,beginRange):
    i = beginRange
    keyWord = keyWord.split()
    keyWordBegIdx = beginRange
    keyWordEndIdx = beginRange
    
    while(i<len(sentence)):
        keyWordBegIdx = i
        tmpObj = getNextWord(sentence,i)
        curWord = tmpObj['curWord']
        i = tmpObj['beginRange']
        flag = 1
        for j in range(0,len(keyWord)):
            if (keyWord[j].lower() not in curWord.lower() and not isSameMeaning(keyWord[j],curWord)):
                flag = 0
                break
            else:
                keyWordEndIdx = i-1
                tmpObj = getNextWord(sentence,i)
                curWord = tmpObj['curWord']
                i = tmpObj['beginRange']
                
        if flag==1:
            return {'beg':keyWordBegIdx,'end':keyWordEndIdx-1}
    return {'beg':-1,'end':-1}

# Function: Try to find structured definition of the keyword
# Input: sentences list & keyword
# Output: The list of items of the keyword if find, otherwise will return an empty list
def tryToFindStructuredDefinition(sentences,keyWord):
    relatedItems = {'key':keyWord,'items':[]}
    for sentence in sentences:
        if '<code>' in sentence or '</code>' in sentence:
            isFind = False
            findRange = 0
            tmpObj = getKeyWordRangeIdx(sentence,keyWord,findRange)
            keyWordBegin = tmpObj['beg']
            keyWordEnd = tmpObj['end']
            while keyWordEnd != -1:                
                
                idx = sentence.find(':',keyWordEnd+1)
                if idx!=-1:
                    isFind = True
                    for i in range(keyWordEnd+1,idx):
                        if (sentence[i]!=' '):
                            isFind = False
                            break
               
                if not isFind:
                    idx = sentence.find('=',keyWordEnd+1)
                    if idx!=-1:
                        isFind = True
                        for i in range(keyWordEnd+1,idx): 
                            if (sentence[i]!=' '):
                                isFind = False 
                                break
                if isFind:
                    sentenceEnd = keyWordEnd+1
                    if sentenceEnd >= len(sentence):
                        sentenceEnd = keyWordEnd
                    while(sentenceEnd < len(sentence)):
                        if(sentence[sentenceEnd]=='\n' or sentence[sentenceEnd]=='.' or sentence[sentenceEnd]==';'):
                            sentenceEnd-=1
                            break
                        sentenceEnd+=1
                    if sentence.find('</code>',keyWordEnd+1)!=-1 and sentence.find('</code>',keyWordEnd+1)<sentenceEnd:
                        sentenceEnd = sentence.find('</code>',keyWordEnd+1)
                        sentenceEnd-=1
                    relatedItems = processStructuredKeyWord(keyWord,sentence[keyWordBegin:sentenceEnd+1])
                    if len(relatedItems['items'])!=0:
                        return relatedItems
                    findRange = sentenceEnd
                    tmpObj = getKeyWordRangeIdx(sentence,keyWord,findRange)
                    keyWordBegin = tmpObj['beg']
                    keyWordEnd = tmpObj['end']
                else:
                    findRange = keyWordEnd
                    tmpObj = getKeyWordRangeIdx(sentence,keyWord,findRange)
                    keyWordBegin = tmpObj['beg']
                    keyWordEnd = tmpObj['end']
    return relatedItems

# Function: check if the two words have the same meaning
def isSameMeaning(word1,word2):
    if '<code>' in word2:
        word2 = word2[word2.find('<code>')+len('<code>'):]
    if '<code>' in word1:
        word1 = word1[word1.find('<code>')+len('<code>'):]
    word1 = word1.lower()
    word2 = word2.lower()
    if (word1 == 'signature' or word1 == 'sig' or word1 == 'sign' or word1 == 'token'):
        if (word2 == 'signature' or word2 == 'sig' or word2 == 'sign' or word2 == 'token'):
            return True
    if (word1 == 'username' or word1 == 'usrname' or word1 == 'mqttusername'):
        if (word2 == 'username' or word2 == 'usrname' or word2 == 'mqttusername'):
            return True
    if (word1 == 'sas' or word1 == 'sharedaccesssignature'):
        if (word2 == 'sas' or word2 == 'sharedaccesssignature'):
            return True
    if (word1 == 'clientid' or word1 == 'mqttclientid'):         #client id, clientid should be added?
        if (word2 == 'clientid' or word2 == 'mqttclientid'):
            return True
    if (word1 == 'password' or word1 == 'mqttpassword'):
        if (word2 == 'password' or word2 == 'mqttpassword'):
            return True
    if(word1.lower()==word2.lower()):
        return True
    return False

# Function: Match keyWord with the tokens in the sentence
# Input: keyWord & tokenList (tokenList is generated by flair.data.Sentence)
# Output: the list of the begin and end index pairs of the token in tokenList
def MatchKeyWordWithTokenList(keyWord, tokenList):
    keyWord = keyWord.split() # keyWord may contain space eg. 'SAS Signature'
    keyWordIdx = 0
    tokenIndexPairList = []
    begIdx = 0
    endIdx = 0
    for i in range(0,len(tokenList)):
        if isSameMeaning(keyWord[keyWordIdx],tokenList[i].text): # eg. signature & sig are the same
            if keyWordIdx == 0:
                begIdx = i
            keyWordIdx+=1
        else:
            keyWordIdx = 0
            begIdx = i+1
            endIdx = i+1
            continue
        if keyWordIdx >= len(keyWord):
            endIdx = i
            keyWordIdx = 0
            tokenIndexPairList.append({'begin':begIdx,'end':endIdx})
            begIdx = i+1
            endIdx = i+1
    return tokenIndexPairList

# Function: get the tag of a token
# Input: token(flair.data.token)
# Output: tag string
def GetTagOfToken(token):
    tags = []
    for label_type in token.annotation_layers.keys():
        if token.get_labels(label_type)[0].value == "O":
            continue
        if token.get_labels(label_type)[0].value == "_":
            continue
        tags.append(token.get_labels(label_type)[0].value)
    all_tags = "<" + "/".join(tags) + ">"
    return all_tags

# Function: Try to find definition of the keyword with nlp method
# Input: sentences list & keyword
# Output: The list of items of the keyword if find, otherwise will return an empty list
def processKeyWordWithNlp(keyWord, sentences):
    relatedItems = {'key':keyWord,'items':[]}
    # DEBUG 
    # print(keyWord)
    # print(':')
    # load the NER tagger
    tagger = MultiTagger.load(['pos-fast', 'frame-fast'])
    for i in range(0,len(sentences)):
        sentenceObj = Sentence(sentences[i])
        # predict NER tags
        tagger.predict(sentenceObj)
        tokens = sentenceObj.tokens
        tokenIndexPairList = MatchKeyWordWithTokenList(keyWord,tokens)
        for j in range(0,len(tokenIndexPairList)):
            keyWordbeginIdx = tokenIndexPairList[j]['begin']
            keyWordendIdx = tokenIndexPairList[j]['end']
            # search forward
            # possible pattern: t1, t2, ...(and) tn in (the) keyword [no in/verb before t1..n]
            k = keyWordbeginIdx-1
            flag = 0 # number of in/verb 
            while k>=0:
                tag = GetTagOfToken(tokens[k])
                if flag == 0 and ('<VB/' in tag or '<VBZ/' in tag):
                    break
                if '<VB/' in tag or ('in' == tokens[k].text and '<IN>' in tag) or '<VBZ/' in tag:
                    flag+=1
                if flag == 0 and '<NNP>' in tag:
                    break
                if flag == 1 and '<NNP>' in tag:
                    relatedItems['items'].append(tokens[k].text)
                if flag > 1 and len(relatedItems['items'])>=1:
                    relatedItems['items'].pop()
                    break
                k-=1
            if len(relatedItems['items'])>0:
                return relatedItems
            # search backward
            # possible pattern: keyword NNs is/has/contains/uses t1, t2, ... and tn [no verb behind t1...n]
            k = keyWordendIdx + 1
            flag = 0 # number of verbs
            while k<len(tokens):
                tag = GetTagOfToken(tokens[k])
                if '<,>' in tag:
                    k+=1
                    continue
                if flag == 0 and ('<NN>' in tag or '<NNP>' in tag):
                    k+=1
                    continue
                if flag == 0 and not('<VBZ/be' in tag or '<VB/contain' in tag or '<VB/use' in tag or '<NN>' in tag or '<NNP>' in tag):
                    break
                if ('<VBZ/be' in tag or '<VB/contain' in tag or '<VB/use' in tag):    
                    flag+=1
                if flag==1 and ('<NNP>' in tag or '<NN>' in tag):
                    curItem = tokens[k].text
                    k+=1
                    tag = GetTagOfToken(tokens[k])
                    while ('<NNP>' in tag or '<NN>' in tag):
                        curItem = curItem +' '+tokens[k].text
                        k+=1
                        if k<len(tokens):
                            tag = GetTagOfToken(tokens[k])
                        else:
                            break
                    relatedItems['items'].append(curItem)
                # process situation like: 'For the Username field, use <code>{iothubhostname}/{device_id}/?api-version=2018-06-30</code>'
                if flag==1 and '<' in tokens[k].text:
                    if (k+2<len(tokens)) and tokens[k+1].text=='code' and '>' in tokens[k+2].text:
                        k=k+3
                        codeItem = ''
                        while (k+2<len(tokens)) and not('</' in tokens[k].text and tokens[k+1].text=='code' and '>' in tokens[k+2].text):
                            codeItem+=tokens[k].text
                            k+=1
                        relatedItems=processStructuredKeyWord(keyWord,keyWord+'='+codeItem)
                        if len(relatedItems['items'])>=1:
                            return relatedItems
                if flag > 1 and len(relatedItems['items'])>=1:
                    relatedItems['items'].pop()
                k+=1
            if len(relatedItems['items'])>0:
                return relatedItems

    return relatedItems

# function: to check if the para is structed
def isStructed(paragraph):
    if "the" in paragraph:
        return False
    elif ''.join(paragraph.split()).startswith('<code>') and paragraph.split().endswith('</code>'):
        return True
    else:
        return False

# function: find the sentences that only has structred data, then call tryToFindStructuredDefinition(sentence, keyWord)
# input: sentences, tabledata, keyword
# output: the related item, a list
def tryToFindStructuredDefinitionOnly(keyWord, sentences, tabledata):
    relatedItems = {'key':keyWord,'items':[]}
    TestSentences = []
    TableConstructed = False  # if the table is structured
    KeywordFlag = False
    StructedFlag = False
    for tableCol in tabledata:
        for tableColdata in tableCol:
            if keyWord.lower() == tableColdata.lower(): # test if the keyword is in rawdata[i][j]
                KeywordFlag = True
            elif isStructed(tableColdata.lower()):
                StructedFlag = True
        if KeywordFlag and StructedFlag:
            TableConstructed = True 
            break

    if not TableConstructed:
        mergedSentences = mergeSentencewithTabledata(sentences,tabledata)
        for sentence in mergedSentences:
            if keyWord.lower() in sentence.lower():
                # DEBUG
                # print("####### keyword sentences")
                # print(sentence)
                TestSentences.append(sentence)
       
       #  print(TestSentences)
        relatedItems = tryToFindStructuredDefinition(TestSentences, keyWord)

    return relatedItems

# function: check if the keyword is the subject of the sentence
# input: a sentence, the keyword
# output: true / false
def KeywordSubjecttoSentence(sentence, keyWord):
    if keyWord in sentence:   
        nlp = StanfordCoreNLP(r'/home/standard-corenlp-full-2021-01-07/')
        # print(nlp.c)
        # print(nlp.word_tokenize(sentence))
        WordList = nlp.word_tokenize(sentence)
        # print(nlp.parse(sentence))
        print ('Dependency Parsing:', nlp.dependency_parse(sentence))
        dependencylist = nlp.dependency_parse(sentence) # a list of turple  [('root',0,9)]

        print ('Part of Speech:', nlp.pos_tag(sentence))

        PoSTagList = nlp.dependency_parse(sentence)
        nlp.close()
        rootIndex = dependencylist[0][2]
        nounList = ['NN','NP','NR','NT','NNP']
        if PoSTagList[rootIndex-1][1] in nounList: 
            # print('is noun')
            if isSameMeaning(keyWord, WordList[rootIndex-1]):
                return True
            elif isSameMeaning('format',  WordList[rootIndex-1]): 
                    for item in dependencylist:
                        if ('nmod' in item or 'compound' in item) and anchorIndex in item: 
                            if isSameMeaning(keyWord, WordList[item[3-(item.index(rootIndex))]-1]): 
                                return True
        else:
            print("not noun")
            for item in dependencylist:  
                if 'nsubj' in item and rootIndex in item:
                    print(item)
                    anchorIndex = item[3-(item.index(rootIndex))]
                    print(anchorIndex)
                    print(WordList[anchorIndex-1])
                    if isSameMeaning(keyWord, WordList[anchorIndex-1]):
                        return True
                    elif isSameMeaning('format', WordList[anchorIndex-1]):
                        print(WordList[anchorIndex-1])
                        for item2 in dependencylist:
                            if ('nmod' in item2 or 'compound' in item2) and anchorIndex in item2: 
                                print(item2)
                                print(item2.index(anchorIndex))
                                anchorIndex = item2[3-(item2.index(anchorIndex))]
                                print(anchorIndex)
                                print(WordList[anchorIndex-1])                              
                                if isSameMeaning(keyWord, WordList[anchorIndex-1]):  
                                    return True
                                else:
                                    for item3 in dependencylist:
                                        if 'compound' in item3 and anchorIndex in item3:
                                            anchorIndex = item3[3-(item3.index(anchorIndex))]
                                            print(WordList[anchorIndex-1])                              
                                            if isSameMeaning(keyWord, WordList[anchorIndex-1]):  
                                                return True
        # print("yes")
        return False
    else: 
        return False


# function: find the sentences that only has structred data and NLP data
# input: sentences, tabledata, keyword
# output: the related item, a list
# a function should be added that the NLP data are related to the keyword or it can be handled in findsentencecontainkeyword
def tryToFindStrucredDefinitionwithNLP(keyWord, sentences, tabledata):
    relatedItems = {'key':keyWord,'items':[]}
    mergedSentences = mergeSentencewithTabledata(sentences,tabledata)
    splitList = [":"]
    for splititem in splitList:
        for sentence in mergedSentences:
            if splititem in sentence:
                nlpdata = sentence.split(splititem)[0]
                codedata = sentence.split(splititem)[1]
                if "<code>" in codedata and "</code>" in codedata:
                    codedatas = re.findall('<code>([\s\S]*)</code>',codedata)
                    DigitFlag = re.search(r'[0-15]\d{4,10}$',codedatas[0])  
                    # print(DigitFlag)
                    if DigitFlag:
                        # print("yes")
                        continue
                    else:
                        # print("nlpdata is " + nlpdata)
                        # print("codedata is" + codedata)
                        # print(sentence)
                        if KeywordSubjecttoSentence(nlpdata,keyWord): 
                            # codedatas = re.findall('<code>([\s\S]*)</code>',codedata)
                            print("nlpdata is " + nlpdata)
                            print("codedata is" )
                            print(codedatas[0])
                            relatedItems = processStructuredKeyWord(keyWord, codedatas[0])
                        else:
                            continue
                    # DEBUG
                    # print("####### sentence has = or :")
                    # print(sentence)
                else:
                    continue
    return relatedItems

# function: turn the table data into simple sentences, so they can be handled with NLP
# input: sentences, tabledata
# output: a merged sentences (no <>)
def mergeSentencewithTabledata(sentences,tabledata):
    mergedSentence = []
    for tableCol in tabledata:
        for tableColdata in tableCol:
            splitedSentence = splitParagraphIntoSentences(tableColdata)
            # DEBUG
            # print("#############")
            # print(mergedSentence)
            mergedSentence.extend(splitedSentence)
    mergedSentence.extend(sentences)
    return mergedSentence

# function: load the table data
# input: filename
# output: return the data
def loadpicklefile(filename):
    fr = open(filename,'rb')
    tabledata = pickle.load(fr)
    fr.close()
    return tabledata

# function check if two words/phrase has the same meaning
def isSameKeyWord(word1,word2):
    word1 = word1.lower()
    word2 = word2.lower()
    if word1 == word2:
        return True
    # SAS
    if ('sas' in word1 or 'sharedaccesssignature' in word1) and ('sasl' not in word1):
        if ('sas' in word2 or 'sharedaccesssignature' in word2) and ('sasl' not in word2):
            return True
    # username
    if 'username' in word1 or 'usrname' in word1:
        if 'username' in word2 or  'usrname' in word2:
            return True
    # password
    if 'password' in word1:
        if 'password' in word2:
            return True
    # clientId
    if 'clientid' in word1 or word1.find('client id')!=-1:
        if 'clientid' in word2 or word2.find('client id')!=-1:
            return True
    return False

# function: check if the item is already in the keyword list
def isAlreadyInKeyWords(item,keyWords):
    for keyWord in keyWords:
        if isSameKeyWord(item,keyWord):
            return True
    return False

def isValidDate(str):
  try:
    time.strptime(str, '%Y-%m-%d')
    return True
  except:
    return False

# function: add new keywords to the original keyWords through related items
# input: current_keyword_list[to prevent repetition], related_items
# output: a list of new keywords list
def createNewKeyWordsList(keyWords,relatedItems):
    for item in relatedItems['items']:
        if isinstance(item, str):
            if (not isAlreadyInKeyWords(item,keyWords)) and (not item.isdigit()) and (not isValidDate(item)):
                keyWords.append(item)
        else:
            keyWords=createNewKeyWordsList(keyWords,item)
    return keyWords


# function: create string version of relatedItems
# Note: keyType = 0 [Outter Equation] keyType = 1 [Inner Equation] keyType = 2 [Function]
def createRelatedItemsString(relatedItems,keyType):
    if keyType == 0:
        res = relatedItems['key']+' : '
    elif keyType == 1:
        res = '{' + relatedItems['key'] + '='
    elif keyType == 2:
        res = relatedItems['key']+'('
    
    for item in relatedItems['items']:
        if isinstance(item, str) and (keyType==0) and item!='':
            if relatedItems['key']!='topic':
                res += '{'+item+'}+'
            else:
                res += '{'+item+'}/'
        elif isinstance(item, str) and (keyType==1) and item!='':
            res += item+'&'
        elif isinstance(item, str) and item!='':
            res += item+','
        elif not isinstance(item, str):
            if ('sign' in item['key']) or ('hmac' in item['key']):
                res += createRelatedItemsString(item,2) 
            else:
                res += createRelatedItemsString(item,1)

            if keyType == 1:
                res += '&'
            elif keyType == 0 and relatedItems['key']!='topic':
                res += '+'
            elif keyType == 0:
                res += '/'
            else:
                res += ','
            
    res = res[0:len(res)-1]
    if keyType == 1:
        res += '}'
    if keyType == 2:
        res += ')'
    return res



def searchCompleteDefinitionHelper(allDefinitions,keyWord):
    keyWordDef = {'key':keyWord,'items':[]}
    for definition in allDefinitions:
        if keyWord.lower() in definition['key'].lower():
            keyWordDef['items'] = definition['items']
            return keyWordDef
    return keyWordDef


# Function: return a diction of the definition of the keyword
# Notice: Sometimes circular definiton will occur, and the definiton of the items that is circualr defined will begin with ###circular definition
def searchCompleteDefinition(allDefinitions,keyWord):
    return searchCompleteDefinitionInner(allDefinitions,keyWord,set())['def']

# Notice: alreadyExistedKeys is used to prevent circular definition
def searchCompleteDefinitionInner(allDefinitions,keyWord,alreadyExistedKeys):
    keyWordDef = {'key':'','items':list()}
    if isinstance(keyWord,str):
        keyWordDef = copy.deepcopy(searchCompleteDefinitionHelper(allDefinitions,keyWord))
        alreadyExistedKeys.add(keyWord)
    else:
        keyWordDef = copy.deepcopy(keyWord)
        alreadyExistedKeys.add(keyWordDef['key'])
    if len(keyWordDef['items']) == 0:
        if len(alreadyExistedKeys)>1:
            return {'def':keyWordDef['key'],'keys':alreadyExistedKeys}
        else:
            return {'def':keyWordDef,'keys':alreadyExistedKeys}
    for i in range(0,len(keyWordDef['items'])):
        if isinstance(keyWordDef['items'][i],str) and keyWordDef['items'][i] in alreadyExistedKeys:
            keyWordDef['items'][i] = '###circular definition_'+keyWordDef['items'][i]
            continue
        resObj = searchCompleteDefinitionInner(allDefinitions,keyWordDef['items'][i],alreadyExistedKeys)
        keyWordDef['items'][i] = resObj['def']
        alreadyExistedKeys = resObj['keys']
    return {'def':keyWordDef,'keys':alreadyExistedKeys}



####################################################
#                    main procedure                #
####################################################

def main():
    dictProperties=Properties("build/serverjs.properties").getProperties()
    # DEBUG
    keyWords = ['clientId', 'password', 'username' ,'topic','payload','content']
    # keyWords = ['SAS token']
    # keyWords = ['a']
    # DEBUG
    # tests = [{'key':'ClientId','sentence':'mqttClientId: clientId+"|sec=3,sigmeth=hmacsha1,time=132323232|"'},{'key':'password','sentence':'mqttPassword: sign_hmac(dSec,content)sign'},{'key': 'SAS','sentence':'SAS sig={sig-str}&se={expiry}&skn={po}&sr={URL-enc}'},{'key':'Username','sentence':'mqttUsername: dName+"&"+pKey'}]
    # for test in tests:
    #     print(processStructuredKeyWord(test['key'],test['sentence']))
    # targetfile = "doc2"
    # targetfile = "./alimqtt/alidoc2"    #  input files
    # targetfile = "./other platforms/alicoap data/Establish connections over CoAP - Device Access_ Alibaba Cloud Documentation Center"
    # targethtml = "./alimqtt/alidoc2.html"
    # parser = argparse.ArgumentParser(description="NLP based semantics extraction method in MPInspector")
    # parser.add_argument('-df','--docfile')
    # args = parser.parse_args()
    # print(args)
    targethtml = dictProperties['htmlpath']
    # targethtml = args.docfile
    print(targethtml)
    targetfile = preprocess(targethtml)
    print(targetfile)
    outputfile = open(targetfile+'_cfe.output', "w")  
    allDefinitions = []
    relatedItems = []
    i = 0
    while i<len(keyWords):
        keyWord = keyWords[i]
        print("######### testing " + keyWord)
        sentences = findSentencesContainKeyWords(targetfile+'result.txt',keyWord)
        tabledata = findTabledatacontainKeywords(targetfile+"table.txt",keyWord)
        ### now we have sentences that contains keyword and table data that contains keyword
        ### First we try to find StructureDefinitionOnly and get the keyword semantics
        relatedItems = tryToFindStructuredDefinitionOnly(keyWord,sentences, tabledata)
        if len(relatedItems['items']) == 0:
            relatedItems = tryToFindStrucredDefinitionwithNLP(keyWord,sentences,tabledata)
            if len(relatedItems['items']) == 0:
                # add the value of table data to sentences
                mergedsentences = mergeSentencewithTabledata(sentences,tabledata)
                relatedItems = processKeyWordWithNlp(keyWord,sentences)
        # relatedItems = tryToFindStructuredDefinition(keyWord,sentences)
        # relatedItems = processKeyWordWithNlp(keyWord,sentences)
        if len(relatedItems['items']) != 0:
            allDefinitions.append(relatedItems)
        # DEBUG
        print(keyWord)
        print(':')
        print(relatedItems)
        print(createRelatedItemsString(relatedItems,0))
        outputfile.write(keyWord)
        outputfile.write('\n:\n')
        outputfile.write(json.dumps(relatedItems))
        outputfile.write('\n\n')
        outputfile.write(createRelatedItemsString(relatedItems,0)+'\n')
        outputfile.write('---------------------------------------------------\n')
        keyWords = createNewKeyWordsList(keyWords,relatedItems)
        i+=1
    outputfile.write('---------------------------------------------------\n')
    outputfile.write('\npassword def dict:\n')
    passWordDef = searchCompleteDefinition(allDefinitions,'password')
    print(passWordDef)
    outputfile.write(json.dumps(passWordDef))
    outputfile.close()


if __name__ == '__main__':
    main()

