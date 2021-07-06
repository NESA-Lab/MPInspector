#!/usr/bin/python

import graphviz
import sys
import re
import copy
import time
from pyparsing import Word, alphas, alphanums, dictOf, nestedExpr, originalTextFor

class autoGCP:
	def __init__(self,dot):
		self.dot = dot
		# facts corresponding to each rule
		self.Establish = {
			'init_server':['St_sev_prev_0_Init','!Server'],
			'init_dev':['St_dev_prev_0_Init','Factory_Provision'],
			'register_dev_in_serv':['St_sev_prev_0_Init','St_dev_prev_0_Init','!Server','Factory_Provision','!St_serv_0_Init','!St_dev_0_Init'],
			'magical_process_of_key_negotiation':['!St_serv_0_Init','!St_dev_0_Init','St_serv_prev_1_KeyNegotiated','St_dev_prev_1_KeyNegotiated','!St_KeyNegotiated'],
			'dev_send_connect':[('St_dev_prev_1_KeyNegotiated','!St_dev_0_Init'),'St_dev_1_0_WaitConnack'],
			'serv_send_connack':[('St_serv_prev_1_KeyNegotiated','!St_serv_0_Init'),'St_serv_1_Connected'],
			'dev_recv_connack':['St_dev_1_0_WaitConnack','St_dev_1_Connected'],
			'dev_recv_connack_fail':'St_dev_1_0_WaitConnack',
			'dev_send_disconnect':'St_dev_1_Connected',
			'server_recv_disconnect':'St_serv_1_Connected',
			'serv_recv_connect_again':['St_serv_1_Connected','St_Close_Connection'],
			'dev_connection_closed_by_server':['St_Close_Connection','St_dev_1_Connected'],
			'dev_send_subscribe':['St_dev_1_Connected','St_dev_2_0_WaitSuback'],
			'serv_send_suback':['St_serv_1_Connected','St_serv_2_Subcribed'],
			'dev_recv_suback':['St_dev_2_0_WaitSuback','St_dev_2_Subscribed'],
			'dev_send_unsubcribe':['St_dev_2_Subscribed','St_dev_2_1_WaitUnsuback'],
			'serv_send_unsuback':['St_serv_2_Subcribed','St_serv_1_Connected'],
			'dev_recv_unsuback':['St_dev_2_1_WaitUnsuback','St_dev_1_Connected'],
			'dev_send_publish':['St_dev_1_Connected','St_dev_3_0_WaitPuback'],
			'serv_send_puback':'St_serv_1_Connected',
			'dev_recv_puback':['St_dev_3_0_WaitPuback','St_dev_1_Connected'],
			'reveal_dev_sk':'St_dev_prev_0_Init',
			'reveal_tls_key':'!St_KeyNegotiated'
		}
		# Parameter passing between different facts
		self.StatePass = {
			'St_sev_prev_0_Init':'St_serv_0_Init',
			'St_dev_prev_0_Init':'St_dev_0_Init',
			'St_serv_0_Init':'St_serv_prev_1_KeyNegotiated',
			'St_dev_0_Init':'St_dev_prev_1_KeyNegotiated',
			'St_serv_prev_1_KeyNegotiated':'St_serv_1_Connected',
			'St_dev_prev_1_KeyNegotiated':'St_dev_1_0_WaitConnack',
			'St_dev_1_0_WaitConnack':'St_dev_1_Connected',
			'St_serv_1_Connected':['St_Close_Connection','St_serv_2_Subcribed'],
			'St_dev_1_Connected':['St_dev_2_0_WaitSuback','St_dev_3_0_WaitPuback'],
			'St_dev_2_0_WaitSuback':'St_dev_2_Subscribed',
			'St_dev_2_Subscribed':'St_dev_2_1_WaitUnsuback'
		}
		# Terms corresponding to each fact
		self.argumentModify = {
			'St_sev_prev_0_Init':[],
			'St_dev_prev_0_Init':[],
			'!Server':[],
			'!St_serv_0_Init':[],
			'!St_dev_0_Init':[],
			'Factory_Provision':[],
			'St_serv_prev_1_KeyNegotiated':[],
			'St_dev_prev_1_KeyNegotiated':[],
			'St_KeyNegotiated':[],
			'St_serv_1_Connected':[],
			'St_dev_1_0_WaitConnack':[],
			'St_dev_1_Connected':[],
			'St_Close_Connection':[],
			'St_serv_2_Subcribed':[],
			'St_dev_2_Subscribed':[],
			'St_dev_2_1_WaitUnsuback':[],
			'St_dev_2_0_WaitSuback':[],
			'St_dev_3_0_WaitPuback':[]
		}
		# Contents of crucial terms
		self.arguments = {
			'clientID':'',
			'username':'',
			'password':'',
			'pkDev':'',
			'skDev':'',
			'tokenKey':'',
			'ur':''
		}
		self.edge = {}
		self.rule = []		# store rules
		self.func = []		# store functions
		self.general = []	# store general information
		self.lemma = []		# store lemmas
		self.label = ['Start_Server_1','Start_Dev_1','Start_Server_2_Create_Dev','Start_Dev_2_WaitConnect','Start_Serv_3_Connack','Start_Dev_3_Connack','Start_Dev_4_WaitSubscribe','Start_Serv_4_Suback','Start_Dev_5_Subscribed','Start_Dev_6_WaitUnsuback','Start_Serv_5_Unsuback','Start_Dev_7_Unsubscribed','Secret','Reveal','Honest','K','Commit','Create','Running']
	
	# function: preprocess dot graph and extract transition edge
	# input: dot graph
	# output: transition edge
	def preprocess(self):
		temp = self.dot.split('\n')
		for i in range(len(temp)):
			if 'label=' in temp[i] and '->' in temp[i]:
				temp[i] = temp[i].strip().strip('/').strip().replace("\\n", ' ').replace(';','').replace(' ','')
				searchResult = re.search("label=\"(.*?)\"",temp[i])
				searchResult_part = searchResult.group(1)
				if 'Init' in temp[i]:
					self.edge['Init'] = searchResult_part
				elif 'KEYEXCHANGE' in temp[i]:
					self.edge['KEYEXCHANGE'] = searchResult_part
				elif 'CONNECT' in temp[i] and 'CONNECT' not in self.edge:
					self.edge['CONNECT'] = searchResult_part
				elif 'DISCONNECT' in temp[i] and 'DISCONNECT' not in self.edge:
					self.edge['DISCONNECT'] = searchResult_part
				elif 'PUBLISH' in temp[i] and 'PUBLISH' not in self.edge:
					self.edge['PUBLISH'] = searchResult_part
				elif 'UNSUBSCRIBE' in temp[i]:
					self.edge['UNSUBSCRIBE'] = searchResult_part
				elif 'SUBSCRIBE' in temp[i] and 'UNSUBSCRIBE' not in temp[i]:
					self.edge['SUBSCRIBE'] = searchResult_part
		
	# function: generate the header of Tamarin code
	# input: none
	# output: the header of Tamarin code
	def General(self,tamarinName):
		self.general.append('theory '+tamarinName+'IoT')
		self.general.append('begin')
		self.general.append('builtins: asymmetric-encryption,multiset,symmetric-encryption,signing,hashing,diffie-hellman')
	
	# function: Initialization of client and server
	# input: the 'Init' edge
	# output: (rule) Init_dev / Init_server / register_dev_in_serv
	def Init(self):
		label = self.edge['Init']
		label = label.replace('),',') ')
		store = ''		
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		for i in range(0,3):
			args = arglist.parseString(label)[i][1]
			separateArgs = []
			for i in range(len(args)):
				if isinstance(args[i],str):
					separateArgs += args[i].strip(',').split(',')
				else:
					separateArgs.append([args[i][0]])
			for i in range(len(separateArgs)):
				if i+1<=len(separateArgs)-1 and isinstance(separateArgs[i],str) and not isinstance(separateArgs[i+1],str):
					if 'clientID' in separateArgs[i]:
						self.arguments['clientID'] = '<'+','.join(['~'+separateArgs[i+1][0].split(',')[j] for j in range(len(separateArgs[i+1][0].split(',')))])+'>'
					if 'username' in separateArgs[i]:
						self.arguments['username'] = '<'+','.join(['~'+separateArgs[i+1][0].split(',')[j] for j in range(len(separateArgs[i+1][0].split(',')))])+'>'
				elif (i+1<=len(separateArgs)-1 and isinstance(separateArgs[i],str) and isinstance(separateArgs[i+1],str)) or (i==len(separateArgs)-1):
					if 'clientID' in separateArgs[i]:
						self.arguments['clientID'] = '~'+separateArgs[i]
					if 'username' in separateArgs[i]:
						self.arguments['username'] = '~'+separateArgs[i]
					if 'password' in separateArgs[i]:
						self.arguments['password'] = '~'+separateArgs[i]
					if 'skDev' in separateArgs[i]:
						self.arguments['skDev'] = '~'+separateArgs[i]
					if 'pkDev' in separateArgs[i]:
						self.arguments['pkDev'] = '~'+separateArgs[i]
						if self.arguments['skDev'] != '':
							self.arguments['pkDev'] = 'pk('+self.arguments['skDev']+')'
					if 'tokenKey' in separateArgs[i]:
						self.arguments['tokenKey'] = '~'+separateArgs[i]
					if 'ur' in separateArgs[i]:
						self.arguments['ur'] = '~'+separateArgs[i]
		self.Init_Dev()
		self.Init_Serv()
		self.Provision()

	def Init_Dev(self):
		FrTemp = []
		argTemp1 = []
		argTemp2 = []
		for term in self.arguments:
			if self.arguments[term] != '':
				if term not in ['pkDev','skDev','ur']:
					if term == 'clientID' and len(self.arguments[term].split(',')) > 1:
						argTemp1.append('clientID')
					else:
						argTemp1.append(self.arguments[term])
				if term != 'pkDev' and term != 'ur':
					if ',' not in self.arguments[term]:
						FrTemp.append('Fr('+self.arguments[term]+')')
					else:
						temp = self.arguments[term].strip('<>').split(',')
						for word in temp:
							FrTemp.append('Fr('+word+')')
		Fr = ','.join(FrTemp)

		letin = ''
		if len(self.arguments['clientID'].split(',')) > 1:
			letin = 'let\n   clientID='+self.arguments['clientID']+'\nin\n'
			self.arguments['clientID'] = 'clientID'

		argTemp2.append(self.arguments['clientID'])
		if self.arguments['pkDev'] != '':
			letin = 'let\n   pkDev='+self.arguments['pkDev']+'\nin\n'
			self.arguments['pkDev'] = 'pkDev'
			argTemp2.append(self.arguments['pkDev'])
		arg2 = ','.join(argTemp2)
		
		if self.arguments['skDev'] != '' and self.arguments['pkDev'] != '':
			argTemp1.append('<'+self.arguments['skDev']+','+self.arguments['pkDev']+'>')
		elif self.arguments['skDev'] != '' and self.arguments['pkDev'] == '':
			argTemp1.append(self.arguments['skDev'])
		arg1 = ','.join(argTemp1)
		
		self.rule.append('rule init_dev:\n'+letin+'[ '+Fr+' ]--[ '+self.label[1]+'('+self.arguments['clientID']+') ]->[ '+self.Establish['init_dev'][0]+'('+arg1+'),'+self.Establish['init_dev'][1]+'('+arg2+') ]')
		self.argumentModify[self.Establish['init_dev'][0]] = arg1
		self.argumentModify[self.Establish['init_dev'][1]] = arg2

	
	def Init_Serv(self):
		FrTemp = []
		argTemp = []
		if self.arguments['ur'] != '':
			FrTemp.append('Fr('+self.arguments['ur']+')')
			argTemp.append(','+self.arguments['ur'])
		Fr = ','.join(FrTemp)
		if len(argTemp) > 0: arg = argTemp[0]
		else: arg = ''
		self.rule.append('rule init_server:\n   [ '+Fr+' ]--[ '+self.label[0]+'($Server) ]->[ '+self.Establish['init_server'][0]+'($Server),'+self.Establish['init_server'][1]+'($Server'+arg+') ]')	
		self.argumentModify[self.Establish['init_server'][0]] = '$Server'
		self.argumentModify[self.Establish['init_server'][1]] = '$Server'+arg

	def Provision(self):
		arg1 = self.argumentModify[self.Establish['register_dev_in_serv'][0]]
		arg2 = self.argumentModify[self.Establish['register_dev_in_serv'][1]]
		arg3 = self.argumentModify[self.Establish['register_dev_in_serv'][2]]
		arg4 = self.argumentModify[self.Establish['register_dev_in_serv'][3]]
		argTemp1 = ['$Server']
		argTemp2 = ['$Server']
		for term in self.arguments:
			if self.arguments[term] != '' and term not in ['skDev','tokenKey']:
				argTemp1.append(self.arguments[term])
			if self.arguments[term] != '' and term not in ['skDev','pkDev']:
				argTemp2.append(self.arguments[term])
		arg5 = ','.join(argTemp1)
		if self.arguments['skDev'] != '' and self.arguments['pkDev'] != '':
			argTemp2.append('<'+self.arguments['skDev']+','+self.arguments['pkDev']+'>')
		elif self.arguments['skDev'] != '' and self.arguments['pkDev'] == '':
			argTemp2.append(self.arguments['skDev'])
		arg6 = ','.join(argTemp2)
		self.rule.append('rule register_dev_in_serv:\n   [ '+self.Establish['register_dev_in_serv'][0]+'('+arg1+'),'+self.Establish['register_dev_in_serv'][1]+'('+arg2+'),'+self.Establish['register_dev_in_serv'][2]+'('+arg3+'),'+self.Establish['register_dev_in_serv'][3]+'('+arg4+') ]--[ '+self.label[2]+'($Server) ]->[ '+self.Establish['register_dev_in_serv'][4]+'('+arg5+'),'+self.Establish['register_dev_in_serv'][5]+'('+arg6+') ]') 
		self.argumentModify[self.Establish['register_dev_in_serv'][4]] = arg5
		self.argumentModify[self.Establish['register_dev_in_serv'][5]] = arg6
	
	# function: key negotiation between client and server
	# input: the 'KEYEXCHANGE' edge
	# output: (rule) magical_process_of_key_negotiation	
	def Negotiation(self):
		if 'KEYEXCHANGE' in self.edge:
			label = self.edge['KEYEXCHANGE']
			ident = Word(alphas,alphanums+"_")
			arglist = dictOf(ident, nestedExpr("(",")"))
			args = arglist.parseString(label)[0][1]
			sessionKey = args[0]
			self.arguments['session_key'] = '~'+sessionKey
			arg1 = self.argumentModify[self.Establish['magical_process_of_key_negotiation'][0]]
			arg2 = self.argumentModify[self.Establish['magical_process_of_key_negotiation'][1]]
			arg3 = arg1+','+self.arguments['session_key']
			arg4 = arg2+','+self.arguments['session_key']
			argTemp1 = ['$Server',self.arguments['clientID'],self.arguments['session_key']]
			arg5 = ','.join(argTemp1)
			self.rule.append('rule magical_process_of_key_negotiation:\n   [ '+self.Establish['magical_process_of_key_negotiation'][0]+'('+arg1+'),'+self.Establish['magical_process_of_key_negotiation'][1]+'('+arg2+'),Fr('+self.arguments['session_key']+') ]--[ ]->[ '+self.Establish['magical_process_of_key_negotiation'][2]+'('+arg3+'),'+self.Establish['magical_process_of_key_negotiation'][3]+'('+arg4+'),'+self.Establish['magical_process_of_key_negotiation'][4]+'('+arg5+') ]')
			self.argumentModify[self.Establish['magical_process_of_key_negotiation'][2]] = arg3
			self.argumentModify[self.Establish['magical_process_of_key_negotiation'][3]] = arg4
			self.argumentModify[self.Establish['magical_process_of_key_negotiation'][4]] = arg5
	
	# function: generate Honest labels
	def Honest(self):
		return self.label[-5]+'(<\'dev\','+self.arguments['clientID']+'>),'+self.label[-5]+'(<\'serv\',$Server>)'
	
	# function: generate Secret labels
	def Secret_Label(self,typeof,res):
		secretTemp = []
		if typeof == 'dev':
			temp = self.arguments['clientID']
		else:
			temp = '$Server'
		for i in range(len(res)):
			secretTemp.append(self.label[-7]+"(<'"+typeof+"',"+temp+">,'"+res[i][0]+"',"+res[i][1]+")")
		return ','.join(secretTemp)
	
	# function: generate Agreement labels	
	def Agreement_Label(self,way,arg1,arg2,arg3,arg4):
		dev_label_send = self.label[-2]+"('"+way+"','dev',"+self.arguments['clientID']+"),"+self.label[-1]+"("+self.arguments['clientID']+",$Server, <'serv','dev',<'"+arg1+"',"+arg2+">>)"
		serv_label = self.label[-2]+"('"+way+"','serv',$Server),"+self.label[-3]+"($Server,"+self.arguments['clientID']+",<'serv','dev',<'"+arg1+"',"+arg2+">>),"+self.label[-1]+"($Server,"+self.arguments['clientID']+", <'dev','serv', <'"+arg1+"',"+arg2+">>),"+self.label[-1]+"($Server,"+self.arguments['clientID']+",<'dev','serv',<'"+arg3+"',"+arg4+">>)"
		dev_label_recv = self.label[-3]+"("+self.arguments['clientID']+",$Server, <'dev', 'serv', <'"+arg1+"', "+arg2+">>),"+self.label[-3]+"("+self.arguments['clientID']+",$Server, <'dev', 'serv', <'"+arg3+"',"+arg4+">>)"
		return [dev_label_send,serv_label,dev_label_recv]
	
	# function: the connection of client and server
	# input: the 'CONNECT' edge
	# output: (rule) dev_send_connect / serv_send_connack / dev_recv_connack
	def Connect(self):
		label = self.edge['CONNECT']
		label = label.replace('/',' ')
		label = label.replace('),',') ')
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		connectArgs = arglist.parseString(label)[0][1]
		connackArgs = arglist.parseString(label)[1][1]
		temp = []
		new = []
		for term in connectArgs:
			if '{' in term:
				term = term.split('{')[1]
			if '}' in term:
				term = term.split('}')[0]
			if isinstance(term,str) and ',' in term:
				for obj in term.split(','):
					temp.append(obj)
			elif isinstance(term,str) and ',' not in term and term != '':
				temp.append(term)
			else:
				temp.append([term[i] for i in range(len(term))])
		for i in range(len(temp)):
			if isinstance(temp[i],str) and self.arguments[temp[i]] == '':
				if temp[i] == 'username':
					new += ['~'+temp[i+1][0].split(',')[j] for j in range(len(temp[i+1][0].split(',')))]
					tmp = ','.join(['~'+temp[i+1][0].split(',')[j] for j in range(len(temp[i+1][0].split(',')))])
					self.arguments[temp[i]] = '<'+tmp+'>'
				elif temp[i] == 'password':
					if 'sas' in temp[i+1]:
						self.func.append('functions: sas/2, get_sas_data/1, sas_verify/3')
						self.func.append('equations: get_sas_data(sas(data, key))=data')
						self.func.append('equations: sas_verify(sas(data, key), data, pk(key)) = true')
						new += ['~'+temp[i+1][1][0].split(',')[j] for j in range(len(temp[i+1][1][0].split(',')))]
						tmp = ','.join(['~'+temp[i+1][1][0].split(',')[j] for j in range(len(temp[i+1][1][0].split(',')))])
						password = 'sas(<'+tmp+'>,'+self.arguments['skDev']+')'
						self.arguments['password'] = password
					if 'JWT' in temp[i+1]:
						self.func.append('functions: jwt/3, get_jwt_data/1, jwt_verify/3, jwt_fmt/1')
						self.func.append('equations: get_jwt_data(jwt(data, key, fmt))=data')
						self.func.append('equations: jwt_verify(jwt(data, key, fmt), data, pk(key)) = true')
						self.func.append('equations: jwt_fmt(jwt(data, key, fmt)) = fmt')
						for j in range(len(temp[i+1][1])):
							if j+1<len(temp[i+1][1]) and isinstance(temp[i+1][1][j],str) and isinstance(temp[i+1][1][j+1][0],str):
								new += ['~'+temp[i+1][1][j+1][0].split(',')[k] for k in range(len(temp[i+1][1][j+1][0].split(',')))]
								tmp = ','.join(['~'+temp[i+1][1][j+1][0].split(',')[k] for k in range(len(temp[i+1][1][j+1][0].split(',')))])
								token = '<'+tmp+'>'
								self.arguments[temp[i+1][1][j]] = token
						password = 'jwt('+token+',~'+temp[i+1][1][-1].split(',')[0]+',~'+temp[i+1][1][-1].split(',')[1]+')'
						new.append('~'+temp[i+1][1][-1].split(',')[1])
						self.arguments['password'] = password
					if 'hashmac' in temp[i+1][0]:
						tmp = '<'+','.join(['~'+temp[i+1][0].split('{')[1].split('}')[0].split(',')[k] for k in range(len(temp[i+1][0].split('{')[1].split('}')[0].split(',')))]) + '>'
						password = 'senc('+tmp+',~'+temp[i+1][0].split('{')[1].split('}')[1]+')'
						self.arguments['password'] = password
		for term in connackArgs:
			if '{' not in term and '}' not in term:
				if 'session_key' in self.arguments:
					connack = 'senc('+term+','+self.arguments['session_key']+')'
				else:
					connack = term
		new = list(set(new))
		FrTemp = []
		for i in range(len(new)):
			FrTemp.append('Fr('+new[i]+')')
		Fr = ','.join(FrTemp)
		if Fr != '': Fr += ','
		if 'session_key' not in self.arguments:
			arg1 = self.argumentModify[self.Establish['dev_send_connect'][0][1]]
			arg2 = self.argumentModify[self.Establish['serv_send_connack'][0][1]]
			fact1 = self.Establish['dev_send_connect'][0][1]
			fact2 = self.Establish['serv_send_connack'][0][1]
		else:
			arg1 = self.argumentModify[self.Establish['dev_send_connect'][0][0]]
			arg2 = self.argumentModify[self.Establish['serv_send_connack'][0][0]]
			fact1 = self.Establish['dev_send_connect'][0][0]
			fact2 = self.Establish['serv_send_connack'][0][0]
		connectTemp = []
		for term in ['clientID','username','password']:
			if self.arguments[term] != '':				
				connectTemp.append(self.arguments[term])
		connect = '<'+','.join(connectTemp)+'>'
		if 'session_key' in self.arguments:
			connect = 'senc('+connect+','+self.arguments['session_key']+')'
		self.argumentModify[self.Establish['dev_send_connect'][1]] = self.argumentModify[fact1]
		self.argumentModify[self.Establish['serv_send_connack'][1]] = self.argumentModify[fact2]
		self.argumentModify[self.Establish['dev_send_connect'][1]] += ','+connect
		self.argumentModify[self.Establish['serv_send_connack'][1]] += ','+connect
		arg3 = self.argumentModify[self.Establish['dev_recv_connack'][0]]
		
		secrecy_temp1 = []
		secrecy_temp2 = []
		for term in ['skDev','clientID','username','password']:
			if self.arguments[term] != '':
				secrecy_temp1.append([term,self.arguments[term]])
				if term != 'skDev':
					secrecy_temp2.append([term,self.arguments[term]])
		secrecy_label1 = self.Secret_Label('dev',secrecy_temp1)
		secrecy_label2 = self.Secret_Label('serv',secrecy_temp2)
		agreement_label = self.Agreement_Label('con','clientID',self.arguments['clientID'],'connack',connack)
		label1 = self.Honest()+','+secrecy_label1+','+agreement_label[0]+','+self.label[3]+'('+self.arguments['clientID']+')'
		label2 = self.Honest()+','+secrecy_label2+','+agreement_label[1]+','+self.label[4]+'($Server)'
		label3 = self.Honest()+','+secrecy_label1+','+agreement_label[2]+','+self.label[5]+'('+self.arguments['clientID']+')'
		
		self.argumentModify[self.Establish['dev_recv_connack'][1]] = arg3
		self.rule.append('rule dev_send_connect:\n   [ '+Fr+fact1+'('+arg1+') ]--[ '+label1+' ]->[ '+self.Establish['dev_send_connect'][1]+'('+self.argumentModify[self.Establish['dev_send_connect'][1]]+'),Out('+connect+') ]')
		self.rule.append('rule serv_send_connack:\n   [ In('+connect+'),'+fact2+'('+arg2+') ]--[ '+label2+' ]->[ '+self.Establish['serv_send_connack'][1]+'('+self.argumentModify[self.Establish['serv_send_connack'][1]]+'),Out('+connack+') ]')
		self.rule.append('rule dev_recv_connack:\n   [ '+self.Establish['dev_recv_connack'][0]+'('+arg3+'), In('+connack+') ]--[ '+label3+' ]->[ '+self.Establish['dev_recv_connack'][1]+'('+arg3+') ]')
	
	# function: the disconnection of client and server, including normal condition and abnormal condition
	# input: the 'DISCONNECT' edge
	# output: (rule) dev_send_disconnect / server_recv_disconnect / serv_recv_connect_again / dev_connection_closed_by_server
	def Disconnect(self):
		label = self.edge['DISCONNECT']
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		disconnectArgs = arglist.parseString(label)[0][1]
		if 'session_key' not in self.arguments:
			disconnect = disconnectArgs[0]
		else:
			disconnect = 'senc('+disconnectArgs[1]+','+self.arguments['session_key']+')'
		arg1 = self.argumentModify[self.Establish['dev_send_disconnect']]
		arg2 = self.argumentModify[self.Establish['server_recv_disconnect']]
		argTemp1 = [self.arguments['clientID']]
		if self.arguments['pkDev'] != '':
			argTemp1.append(self.arguments['pkDev'])
		if 'session_key' in self.arguments:
			argTemp1.append(self.arguments['session_key'])
		arg3 = ','.join(argTemp1)
		label1 = self.Honest()+','+self.label[-1]+'('+self.arguments['clientID']+',$Server,<\'serv\',\'dev\',<\'discon\','+disconnect+'>>)'
		label2 = self.Honest()+','+self.label[-2]+'(\'discon\',\'dev\','+self.arguments['clientID']+')'+','+self.label[-3]+'($Server,'+self.arguments['clientID']+',<\'serv\',\'dev\',<\'discon\','+disconnect+'>>)'
		self.rule.append('rule dev_send_disconnect:\n   [ '+self.Establish['dev_send_disconnect']+'('+arg1+') ]--[ '+label1+' ]->[ Out('+disconnect+') ]')
		self.rule.append('rule server_recv_disconnect:\n   [ '+self.Establish['server_recv_disconnect']+'('+arg2+'),In('+disconnect+') ]--[ '+label2+' ]->[ ]')
		self.rule.append('rule serv_recv_connect_again:\n   [ '+self.Establish['serv_recv_connect_again'][0]+'('+arg2+'),In(connect) ]--[ ]->[ '+self.Establish['serv_recv_connect_again'][1]+'('+arg3+') ]')
		self.rule.append('rule dev_connection_closed_by_server:\n   [ '+self.Establish['dev_connection_closed_by_server'][0]+'('+arg3+'),'+self.Establish['dev_connection_closed_by_server'][1]+'('+arg1+') ]--[ ]->[ ]')
		self.argumentModify[self.Establish['dev_connection_closed_by_server'][0]] = arg3
	
	# function: participants subscribes to one topic
	# input: the 'SUBSCRIBE' edge
	# output: (rule) dev_send_subscribe / serv_send_suback / dev_recv_suback
	def Subscribe(self):
		label = self.edge['SUBSCRIBE'].replace('/',' ')
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		subscribeArgs = arglist.parseString(label)[0][1]
		subackArgs = arglist.parseString(label)[1][1][0]
		subTemp = []
		flag = 0
		for term in subscribeArgs:
			if '{' in term:
				term = [term.split('{')[1].strip(',')]
			if '}' in term:
				term = term.split('}')[0].strip(',')
				if ',' in term:
					term = term.split(',')
					flag = 1
				else:
					term = [term]
			if isinstance(term,str):
				term = [term.strip(',')]
			if flag == 0:
				subTemp.append(term[0])
			else:
				subTemp.append(term[0])
				subTemp.append(term[1])
				flag = 0
		subscribeTemp = ['~'+subTemp[1].split(',')[i] for i in range(len(subTemp[1].split(',')))]
		topic = '<'+','.join(subscribeTemp)+'>'
				
		if '{' in subackArgs:
			subackTemp = ['~'+subackArgs.split('{')[1].split('}')[0].split(',')[i] for i in range(len(subackArgs.split('{')[1].split('}')[0].split(',')))]
		else:
			subackTemp = ['~'+subackArgs.split(',')[i] for i in range(len(subackArgs.split(',')))]
		if 'session_key' in self.arguments:
			suback = 'senc(<'+','.join(subackTemp)+'>,'+self.arguments['session_key']+')'
		else:
			suback = '<'+','.join(subackTemp)+'>'
					
		args = []
		for i in range(2,4):
			if i < len(subTemp):
				args.append('~'+subTemp[i])
		FrTemp1 = []
		for arg in topic.strip('<>').split(',')+args:
			if arg not in FrTemp1 and arg.strip('~') not in self.arguments:
				FrTemp1.append('Fr('+arg+')')
			if arg.strip('~') == 'qos' or arg.strip('~') == 'packetID':
				self.arguments[arg.strip('~')] = arg
		Fr1 = ','.join(FrTemp1)
		
		Fr2 = ''
		if subackTemp[1] != '~qos':
			Fr2 = 'Fr('+subackTemp[1]+'),'
		
		subscribe = [self.arguments['packetID'],topic,self.arguments['qos']]
		subscribe = '<'+','.join(subscribe)+'>'
		if 'session_key' in self.arguments:
			subscribe = 'senc('+subscribe+','+self.arguments['session_key']+')'
		
		arg1 = self.argumentModify[self.Establish['dev_send_subscribe'][0]]
		arg2 = self.argumentModify[self.Establish['serv_send_suback'][0]]
		arg3 = self.argumentModify[self.Establish['dev_send_subscribe'][0]]+','+'<'+topic+','+self.arguments['qos']+'>'
		arg4 = self.argumentModify[self.Establish['serv_send_suback'][0]]+','+'<'+topic+','+self.arguments['qos']+'>'
		
		secrecy_lemma1 = self.Secret_Label('dev',[['subtopic',topic]])
		secrecy_lemma2 = self.Secret_Label('serv',[['subtopic',topic]])
		secrecy_lemma3 = self.Secret_Label('dev',[['subtopic',topic]])
		agreement_lemma = self.Agreement_Label('sub','subtopic',topic,'suback',suback)
		label1 = self.Honest()+','+secrecy_lemma1+','+agreement_lemma[0]+','+self.label[6]+'('+self.arguments['clientID']+')'
		label2 = self.Honest()+','+secrecy_lemma2+','+agreement_lemma[1]+','+self.label[7]+'($Server)'
		label3 = self.Honest()+','+secrecy_lemma3+','+agreement_lemma[2]+','+self.label[8]+'('+self.arguments['clientID']+')'
		
		self.rule.append('rule dev_send_subscribe:\n   [ '+Fr1+','+self.Establish['dev_send_subscribe'][0]+'('+arg1+') ]--[ '+label1+' ]->[ '+self.Establish['dev_send_subscribe'][1]+'('+arg3+'),Out('+subscribe+') ]')
		self.rule.append('rule serv_send_suback:\n   [ '+Fr2+self.Establish['serv_send_suback'][0]+'('+arg2+'),In('+subscribe+') ]--[ '+label2+' ]->[ '+self.Establish['serv_send_suback'][1]+'('+arg4+'),Out('+suback+') ]')
		self.rule.append('rule dev_recv_suback:\n   [ In('+suback+'),'+self.Establish['dev_recv_suback'][0]+'('+arg3+') ]--[ '+label3+' ]->[ '+self.Establish['dev_recv_suback'][1]+'('+arg3+') ]')
		self.argumentModify[self.Establish['dev_send_subscribe'][1]] = arg3
		self.argumentModify[self.Establish['serv_send_suback'][1]] = arg4
		self.argumentModify[self.Establish['dev_recv_suback'][1]] = arg3
	
	# function: participants unsubscribes to one topic
	# input: the 'UNSUBSCRIBE' edge
	# output: (rule) dev_send_unsubscribe / serv_send_unsuback / dev_recv_unsuback
	def Unsubscribe(self):
		label = self.edge['UNSUBSCRIBE'].replace('/',' ')
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		unsubscribeArgs = arglist.parseString(label)[0][1]
		unsubackArgs = arglist.parseString(label)[1][1][0]
		unsubTemp = []
		flag = 0
		for term in unsubscribeArgs:
			if '{' in term:
				term = term.split('{')[1].strip(',')
				if '}' in term:
					term = term.split('}')[0].strip(',')
					if ',' in term:
						term = term.split(',')
						flag = 1
					else:
						term = [term]
				else:
					term = [term]
			if '}' in term:
				term = term.split('}')[0].strip(',')
				if ',' in term:
					term = term.split(',')
					flag = 1
				else:
					term = [term]
			if isinstance(term,str):
				term = [term.strip(',')]
			if flag == 0:
				unsubTemp.append(term[0])
			else:
				unsubTemp.append(term[0])
				unsubTemp.append(term[1])
				flag = 0
		Fr = ''
		if len(unsubTemp) < 3:
			unsubTemp.insert(1,unsubTemp[0])
			Fr = 'Fr(~'+unsubTemp[0]+'),'
			topic = '~'+unsubTemp[0]
		else:
			unsubscribeTemp = ['~'+unsubTemp[1].split(',')[i] for i in range(len(unsubTemp[1].split(',')))]
			topic = '<'+','.join(unsubscribeTemp)+'>'
			
		unsubscribe = [topic,self.arguments['packetID']]
		unsubscribe = '<'+','.join(unsubscribe)+'>'
		if 'session_key' in self.arguments:
			unsubscribe = 'senc('+unsubscribe+','+self.arguments['session_key']+')'
		
		if '{' in unsubackArgs:
			unsubackTemp = ['~'+unsubackArgs.split('{')[1].split('}')[0].split(',')[i] for i in range(len(unsubackArgs.split('{')[1].split('}')[0].split(',')))]
		else:
			unsubackTemp = ['~'+unsubackArgs.split(',')[i] for i in range(len(unsubackArgs.split(',')))]
		if 'session_key' in self.arguments:
			unsuback = 'senc('+','.join(unsubackTemp)+','+self.arguments['session_key']+')'
		else:
			unsuback = ','.join(unsubackTemp)
		
		arg1 = self.argumentModify[self.Establish['dev_send_unsubcribe'][0]]
		arg2 = self.argumentModify[self.Establish['serv_send_unsuback'][0]]
		arg3 = self.argumentModify[self.Establish['serv_send_unsuback'][1]]
		arg4 = self.argumentModify[self.Establish['dev_recv_unsuback'][1]]
		arg5 = self.argumentModify[self.Establish['dev_recv_unsuback'][1]]+','+topic

		secrecy_lemma1 = self.Secret_Label('dev',[['unsubtopic',topic]])
		secrecy_lemma2 = self.Secret_Label('serv',[['unsubtopic',topic]])
		secrecy_lemma3 = self.Secret_Label('dev',[['unsubtopic',topic]])
		agreement_lemma = self.Agreement_Label('unsub','unsubtopic',topic,'unsuback',unsuback)
		label1 = self.Honest()+','+secrecy_lemma1+','+agreement_lemma[0]+','+self.label[9]+'('+self.arguments['clientID']+')'
		label2 = self.Honest()+','+secrecy_lemma2+','+agreement_lemma[1]+','+self.label[10]+'($Server)'
		label3 = self.Honest()+','+secrecy_lemma3+','+agreement_lemma[2]+','+self.label[11]+'('+self.arguments['clientID']+')'
		
		self.rule.append('rule dev_send_unsubcribe:\n   [ '+Fr+'Fr('+self.arguments['packetID']+'),'+self.Establish['dev_send_unsubcribe'][0]+'('+arg1+') ]--[ '+label1+' ]->[ '+self.Establish['dev_send_unsubcribe'][1]+'('+arg5+'),Out('+unsubscribe+') ]')
		self.rule.append('rule serv_send_unsuback:\n   [ '+self.Establish['serv_send_unsuback'][0]+'('+arg2+'),In('+unsubscribe+') ]--[ '+label2+' ]->[ '+self.Establish['serv_send_unsuback'][1]+'('+arg3+'),Out('+unsuback+') ]')
		self.rule.append('rule dev_recv_unsuback:\n   [ '+self.Establish['dev_recv_unsuback'][0]+'('+arg5+'),In('+unsuback+') ]--[ '+label3+' ]->[ '+self.Establish['dev_recv_unsuback'][1]+'('+arg4+') ]')
	
	# function: participants publish one topic
	# input: the 'PUBLISH' edge
	# output: (rule) dev_send_publish / serv_send_puback / dev_recv_puback	
	def Publish(self):
		label = self.edge['PUBLISH'].replace('/',' ')
		ident = Word(alphas,alphanums+"_")
		arglist = dictOf(ident, nestedExpr("(",")"))
		publishArgs = arglist.parseString(label)[0][1]
		pubackArgs = arglist.parseString(label)[1][1][0]
		flag = 0
		publishTemp = []
		for term in publishArgs:
			if '{' in term:
				term = term.split('{')[1].strip(',').strip('\'')
				if '}' in term:
					term = term.split('}')[0].strip(',').strip('\'')
					if ',' in term:
						term = term.split(',')
						flag = 1
					else:
						term = [term]
				else:
					term = [term]
			if '}' in term:
				term = term.split('}')[0].strip(',').strip('\'')
				if ',' in term:
					term = term.split(',')
					flag = 1
				else:
					term = [term]
			if isinstance(term,str):
				term = [term.strip(',').strip('\'')]
			if flag == 0:
				publishTemp.append(term[0])
			else:
				publishTemp.append(term[0])
				publishTemp.append(term[1])
				flag = 0
		packetID = ''
		qos = ''
		md5 = ''
		FrNew = []
		for i in range(len(publishTemp)):
			if publishTemp[i] == 'topic' and publishTemp[i+1] != 'payload':
				topic = '<'+','.join(['~'+publishTemp[i+1].split(',')[j] for j in range(len(publishTemp[i+1].split(',')))])+'>'
				qos = '~'+publishTemp[i+1].split(',')[0]
				FrNew += ['~'+publishTemp[i+1].split(',')[j] for j in range(len(publishTemp[i+1].split(','))) if publishTemp[i+1].split(',')[j]!='clientID']
			elif publishTemp[i] == 'topic' and publishTemp[i+1] == 'payload':
				topic = '~'+publishTemp[i]
				FrNew.append(topic)
			if publishTemp[i] == 'payload' and publishTemp[i+1] != 'packetID':
				payload = ','.join(['~'+publishTemp[i+1].split('{')[1].split('}')[0].split(',')[j] for j in range(len(publishTemp[i+1].split('{')[1].split('}')[0].split(',')))])
				FrNew += ['~'+publishTemp[i+1].split('{')[1].split('}')[0].split(',')[j] for j in range(len(publishTemp[i+1].split('{')[1].split('}')[0].split(','))) if publishTemp[i+1].split('{')[1].split('}')[0].split(',')[j]!='clientID']
				key = '~'+publishTemp[i+1].split('}')[1]
				payload = 'senc(<'+payload+'>,'+key+')'
			elif publishTemp[i] == 'payload' and publishTemp[i+1] == 'packetID':
				payload = '~'+publishTemp[i]
				FrNew.append(payload)
			if publishTemp[i] == 'md5' and publishTemp[i+1] != 'packetID':
				md5 = ','.join([payload]+['~'+publishTemp[i+1].split('{')[1].split('}')[0].split(',')[j] for j in range(1,len(publishTemp[i+1].split('{')[1].split('}')[0].split(',')))])
				key = publishTemp[i+1].split('}')[1].split(',')
				md5 = '<'+'senc(<'+md5+'>,~'+key[0]+'),~'+key[1]+'>'
			if publishTemp[i] == 'packetID' and i<len(publishTemp):
				packetID = '~'+publishTemp[i]
				FrNew.append(packetID)
		Fr = ','.join(['Fr('+FrNew[i]+')' for i in range(len(FrNew))])
		
		pubtopic = '<'+payload+','+qos+','+packetID+'>'
		
		if md5 == '':
			publish = '<'+','.join([topic,payload,packetID])+'>'
		else:
			publish = '<'+','.join([topic,payload,md5,packetID])+'>'
		if 'session_key' in self.arguments:
			publish = 'senc('+publish+','+self.arguments['session_key']+')'
			
		puback = packetID
		if 'session_key' in self.arguments:
			puback = 'senc('+puback+','+self.arguments['session_key']+')'
			
		arg1 = self.argumentModify[self.Establish['dev_send_publish'][0]]
		arg2 = self.argumentModify[self.Establish['serv_send_puback']]
		arg3 = self.argumentModify[self.Establish['dev_send_publish'][0]]+','+pubtopic
		
		secrecy_lemma1 = self.Secret_Label('dev',[['pubtopic',pubtopic],['payload',payload]])
		secrecy_lemma2 = self.Secret_Label('serv',[['pubtopic',pubtopic],['payload',payload]])
		secrecy_lemma3 = self.Secret_Label('dev',[['pubtopic',pubtopic],['payload',payload]])
		agreement_lemma = self.Agreement_Label('pub','pubtopic',pubtopic,'puback',puback)
		label1 = self.Honest()+','+secrecy_lemma1+','+agreement_lemma[0]
		label2 = self.Honest()+','+secrecy_lemma2+','+agreement_lemma[1]
		label3 = self.Honest()+','+secrecy_lemma3+','+agreement_lemma[2]
		
		self.rule.append('rule dev_send_publish:\n   [ '+Fr+','+self.Establish['dev_send_publish'][0]+'('+arg1+')]--[ '+label1+' ]->[ '+self.Establish['dev_send_publish'][1]+'('+arg3+'),Out('+publish+') ]')
		self.rule.append('rule serv_send_puback:\n   [ '+self.Establish['serv_send_puback']+'('+arg2+'),In('+publish+') ]--[ '+label2+' ]->[ '+self.Establish['serv_send_puback']+'('+arg2+'),Out('+puback+') ]')
		self.rule.append('rule dev_recv_puback:\n   [ In('+puback+'),'+self.Establish['dev_recv_puback'][0]+'('+arg3+') ]--[ '+label3+' ]->[ '+self.Establish['dev_recv_puback'][1]+'('+arg1+') ]')
		self.argumentModify[self.Establish['dev_send_publish'][1]] = arg3
	
	# function: information leakage
	# input: none
	# output: (rule) reveal_dev_sk / reveal_tls_key
	def Reveal(self):
		if self.arguments['skDev'] != '':
			label = self.label[-6]+'(<\'dev\','+self.arguments['clientID']+'>, <\'sk\','+self.arguments['skDev']+'>)'
			arg1 = self.argumentModify[self.Establish['reveal_dev_sk']]
			self.rule.append('rule reveal_dev_sk:\n   [ '+self.Establish['reveal_dev_sk']+'('+arg1+') ]--[ '+label+' ]->[ Out('+self.arguments['skDev']+') ]')
		elif self.arguments['tokenKey'] != '':
			label = self.label[-6]+'(<\'dev\','+self.arguments['clientID']+'>, <\'tokenKey\','+self.arguments['tokenKey']+'>)'
			arg1 = self.argumentModify[self.Establish['reveal_dev_sk']]
			self.rule.append('rule reveal_dev_sk:\n   [ '+self.Establish['reveal_dev_sk']+'('+arg1+') ]--[ '+label+' ]->[ Out('+self.arguments['tokenKey']+') ]')
		else:
			label = self.label[-6]+'(<\'dev\','+self.arguments['clientID']+'>, <\'clientID\','+self.arguments['clientID']+'>)'
			arg1 = self.argumentModify[self.Establish['reveal_dev_sk']]
			self.rule.append('rule reveal_dev_sk:\n   [ '+self.Establish['reveal_dev_sk']+'('+arg1+') ]--[ '+label+' ]->[ Out('+self.arguments['clientID']+') ]')
		
		if 'session_key' in self.arguments:
			arg2 = self.argumentModify[self.Establish['reveal_tls_key']]
			self.rule.append('rule reveal_tls_key:\n   [ '+self.Establish['reveal_tls_key']+'('+arg2+') ]--[ ]->[ Out('+self.arguments['session_key']+') ]')
	
	# function: generate Executability lemmas
	def Lemma_Executability(self):
		temp = []
		for i in range(12):
			if 'serv' in self.label[i].lower():
				temp.append('serv')
			elif 'dev' in self.label[i].lower():
				temp.append('dev')
		self.lemma.append('lemma executability_connection:\n   exists-trace\n"\n   (Ex '+temp[0]+' #i. '+self.label[0]+'('+temp[0]+')@i )\n   & (Ex '+temp[1]+' #i. '+self.label[1]+'('+temp[1]+')@i ) \n   & (Ex '+temp[2]+' #i. '+self.label[2]+'('+temp[2]+')@i)\n   & (Ex '+temp[3]+' #i. '+self.label[3]+'('+temp[3]+')@i )\n   & (Ex '+temp[4]+' #i. '+self.label[4]+'('+temp[4]+')@i)\n   & (Ex '+temp[5]+' #i. '+self.label[5]+'('+temp[5]+')@i)\n"')
		self.lemma.append('lemma executability_subscribe:\n   exists-trace\n"\n   (Ex '+temp[0]+' #i. '+self.label[0]+'('+temp[0]+')@i )\n   & (Ex '+temp[1]+' #i. '+self.label[1]+'('+temp[1]+')@i ) \n   & (Ex '+temp[2]+' #i. '+self.label[2]+'('+temp[2]+')@i)\n   & (Ex '+temp[3]+' #i. '+self.label[3]+'('+temp[3]+')@i )\n   & (Ex '+temp[4]+' #i. '+self.label[4]+'('+temp[4]+')@i)\n   & (Ex '+temp[5]+' #i. '+self.label[5]+'('+temp[5]+')@i)\n   & (Ex '+temp[6]+' #i. '+self.label[6]+'('+temp[6]+')@i)\n   & (Ex '+temp[7]+' #i. '+self.label[7]+'('+temp[7]+')@i)\n   & (Ex '+temp[8]+' #i. '+self.label[8]+'('+temp[8]+')@i)\n   & (Ex '+temp[9]+' #i. '+self.label[9]+'('+temp[9]+')@i)\n   & (Ex '+temp[10]+' #i. '+self.label[10]+'('+temp[10]+')@i)\n   & (Ex '+temp[11]+' #i. '+self.label[11]+'('+temp[11]+')@i)\n"')	
	
	def Secrecy(self,target,message):
		self.lemma.append('lemma secrecy_'+target+'_'+message+':\n"\n   All '+target+' msg #i. '+self.label[-7]+'(<\''+target+'\','+target+'>,\''+message+'\',msg)@i\n   ==> (not (Ex #j. '+self.label[-4]+'(msg)@j)) | (Ex X data #j. '+self.label[-6]+'(X,data)@j & '+self.label[-5]+'(X)@i)\n" ')
	
	# function: generate Secrecy lemmas
	def Lemma_Secrecy(self):
		# connection
		self.Secrecy('dev','skDev')
		self.Secrecy('dev','clientID')
		self.Secrecy('serv','clientID')
		self.Secrecy('dev','username')
		self.Secrecy('serv','username')
		self.Secrecy('dev','password')
		self.Secrecy('serv','password')
		# subscribe
		self.Secrecy('dev','subtopic')
		self.Secrecy('serv','subtopic')
		# unsubscribe
		self.Secrecy('dev','unsubtopic')
		self.Secrecy('serv','unsubtopic')
		# publish
		self.Secrecy('dev','payload')
		self.Secrecy('dev','pubtopic')
		self.Secrecy('serv','payload')
		self.Secrecy('serv','pubtopic')
		
	def Secrecy_AC(self,target,message):
		self.lemma.append('lemma AC_secrecy_'+target+'_'+message+':\n"\n   All '+target+' msg #i. '+self.label[-7]+'(<\''+target+'\','+target+'>,\''+message+'\',msg) @i\n   ==> (not (Ex #j. '+self.label[-4]+'(msg)@j))\n"')
	
	# function: generate Secrecy lemmas (AC:All Compromised)	
	def Lemma_Secrecy_AC(self):
		self.Secrecy_AC('dev','skDev')	
		self.Secrecy_AC('dev','clientID')	
		self.Secrecy_AC('serv','clientID')	
	
	def Agreement(self,source,destination,way,message):
		self.lemma.append('lemma aliveness_'+source+'_'+destination+'_'+message+':\n"\n   All a b t #i. '+self.label[-3]+'(a,b,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@i\n   ==> (Ex id #j. '+self.label[-2]+'(\''+way+'\',\''+destination+'\',id) @j) \n   | (Ex X data #r. '+self.label[-6]+'(X,data)@r & '+self.label[-5]+'(X)@i)\n" ')
		self.lemma.append('lemma weak_agreement_'+source+'_'+destination+'_'+message+':\n"\n   All a b t1 #i. '+self.label[-3]+'(a,b,<\''+source+'\',\''+destination+'\',<\''+message+'\',t1>>)@i\n   ==> (Ex t2 #j. '+self.label[-1]+'(b,a,<\''+source+'\',\''+destination+'\',<\''+message+'\',t2>>)@j) \n   | (Ex X data #r. '+self.label[-6]+'(X,data)@r & '+self.label[-5]+'(X)@i)\n"')
		self.lemma.append('lemma noninjective_agreement_'+source+'_'+destination+'_'+message+':\n"\n   All a b t #i. '+self.label[-3]+'(a,b,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@i\n   ==> (Ex #j. '+self.label[-1]+'(b,a,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@j) \n   | (Ex X data #r. '+self.label[-6]+'(X,data)@r & '+self.label[-5]+'(X)@i)\n"')
		self.lemma.append('lemma injective_agreement_'+source+'_'+destination+'_'+message+':\n"\n   All a b t #i. '+self.label[-3]+'(a,b,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@i\n   ==> (Ex #j. '+self.label[-1]+'(b,a,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@j \n   & j<i \n   & not(Ex a2 b2 #i2. '+self.label[-3]+'(a2,b2,<\''+source+'\',\''+destination+'\',<\''+message+'\',t>>)@i2 & not(#i2 = #i))) \n   | (Ex X data #r. '+self.label[-6]+'(X,data)@r & '+self.label[-5]+'(X)@i)\n"')
	
	# function: generate Agreement lemmas	
	def Lemma_Agreement(self):
		# connection
		self.Agreement('serv','dev','con','clientID')
		# connack
		self.Agreement('dev','serv','con','connack')
		# disconnect
		self.Agreement('serv','dev','discon','discon')
		# subscribe
		self.Agreement('serv','dev','sub','subtopic')
		# suback
		self.Agreement('dev','serv','sub','suback')
		# unsubscribe
		self.Agreement('serv','dev','sub','unsubtopic')
		# unsuback
		self.Agreement('dev','serv','sub','unsuback')
		# publish
		self.Agreement('serv','dev','pub','pubtopic')
		# puback
		self.Agreement('dev','serv','pub','puback')

if __name__ == "__main__":
	print "*-------------------------*"
	print "|    Model Translation    |"
	print "*-------------------------*"
	platformname = input("load state machine name:")
	# for platformname in ['bosch.dot','azure.dot','aws.dot','gcp.dot','tuya.dot']:
	start = time.time()
	with open(platformname) as pic:
		dot_graph = pic.read()
	dot1 = autoGCP(dot_graph)
	dot1.preprocess()
	dot1.General(platformname.split('.')[0])
	dot1.Init()
	dot1.Negotiation()
	dot1.Connect()
	dot1.Disconnect()
	dot1.Subscribe()
	dot1.Unsubscribe()
	dot1.Publish()
	dot1.Reveal()
	dot1.Lemma_Executability()
	dot1.Lemma_Secrecy()
	dot1.Lemma_Secrecy_AC()
	dot1.Lemma_Agreement()
	end = time.time()
	print " "+platformname.split('.')[0]+' IoT: '+str((end-start)/1)+'s'
	# integrate into the file to form Tamarin code
	with open(platformname.split('.')[0]+'.spthy','w') as temp:
			for gen in dot1.general:
				temp.write(gen)
				temp.write('\n')
			temp.write('\n')
			for func in dot1.func:
				temp.write(func)
				temp.write('\n')
			temp.write('\n')
			for rule in dot1.rule:
				temp.write(rule)
				temp.write('\n\n')
			temp.write('\n')
			for lemma in dot1.lemma:
				temp.write(lemma)
				temp.write('\n\n')
			temp.write('\n')
			temp.write('end')