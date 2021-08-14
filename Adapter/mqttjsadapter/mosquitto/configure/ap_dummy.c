#include <mosquitto.h>
#include <mosquitto_broker.h>
#include <mosquitto_plugin.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define EXPORT_DLL
#define LOG(x, ...) printf("[AUTH] "  ## x ## "\n", __VA_ARGS__)

enum password_auth {
	PASSWD_AUTH_NONE,
	PASSWD_AUTH_MATCH,
	PASSWD_AUTH_ALITCP
};

struct userdata { 
	enum password_auth password_auth;
};

int EXPORT_DLL mosquitto_auth_plugin_version(void)
{
	return MOSQ_AUTH_PLUGIN_VERSION;
}



int EXPORT_DLL mosquitto_plugin_init(mosquitto_plugin_id_t* identifier, void** userdata, struct mosquitto_opt* options, int option_count)
{
	struct userdata* ud;
	struct mosquitto_opt* o;

	*userdata = (struct userdata*)malloc(sizeof(struct userdata));
	if (*userdata == NULL) {
		perror("allocting userdata");
		return MOSQ_ERR_UNKNOWN;
	}
	ud = *userdata;

	o = options;
	const char* password_auth = NULL;
	for (int i = 0; i < option_count; ++i, ++o)
	{
		if (!strcmp(o->key, "password_auth"))
		{
			password_auth = o->value;
			ud->password_auth = PASSWD_AUTH_NONE;

			if (!strcmp(o->value, "match")) {
				ud->password_auth = PASSWD_AUTH_MATCH;
			}
			else if (!strcmp(o->value, "alitcp")) {
				ud->password_auth = PASSWD_AUTH_ALITCP;
			}
		}
	}

	LOG("Plugin inited, password_auth:%s", password_auth);
	return MOSQ_ERR_SUCCESS;
}

int mosquitto_auth_plugin_init(void** user_data, struct mosquitto_opt* opts, int opt_count)
{
	if (*user_data == NULL)
	{
		return mosquitto_plugin_init(NULL, user_data, opts, opt_count);
	}
	return MOSQ_ERR_SUCCESS;
}

int EXPORT_DLL mosquitto_auth_plugin_cleanup(void* userdata, struct mosquitto_opt* opts, int opt_count)
{
	struct userdata* ud = (struct userdata*)userdata;

	free(ud);

	return MOSQ_ERR_SUCCESS;
}

int EXPORT_DLL mosquitto_auth_security_init(void* user_data, struct mosquitto_opt* opts, int opt_count, bool reload)
{
	return MOSQ_ERR_SUCCESS;
}

int EXPORT_DLL mosquitto_auth_security_cleanup(void* user_data, struct mosquitto_opt* opts, int opt_count, bool reload)
{
	return MOSQ_ERR_SUCCESS;
}


#if MOSQ_AUTH_PLUGIN_VERSION >=3
int EXPORT_DLL mosquitto_auth_unpwd_check(void* user_data, struct mosquitto* client, const char* username, const char* password)
#else
int mosquitto_auth_unpwd_check(void* userdata, const char* username, const char* password)
#endif
{
	/*
	MOSQ_ERR_SUCCESS if the user is authenticated.  
	MOSQ_ERR_AUTH if authentication failed.  
	MOSQ_ERR_UNKNOWN for an application specific error.  
	MOSQ_ERR_PLUGIN_DEFER if your plugin does not wish to handle this check.
	*/
	struct userdata* ud = user_data;
	LOG("Auth started, password_auth:%d", -1);
	LOG("Auth started, password_auth:%d", (int)ud->password_auth);
	LOG("Auth started, password:%s", password);
	int granted = MOSQ_ERR_SUCCESS;
	if (ud->password_auth != PASSWD_AUTH_NONE && password == NULL)
	{
		LOG("Auth finshed, no password provied, rejected");
		granted = MOSQ_ERR_AUTH;
	}

	else if (ud->password_auth == PASSWD_AUTH_MATCH)
	{
		LOG("Using match");
		if (!!strcmp(password, "mosq_dummy_password")) {
			granted = MOSQ_ERR_AUTH;
			LOG("REJECTED by match");
		}
	}

	return granted;
}

#if MOSQ_AUTH_PLUGIN_VERSION >= 3
int EXPORT_DLL mosquitto_auth_acl_check(void* user_data, int access, struct mosquitto* client, const struct mosquitto_acl_msg* msg)
#else
int mosquitto_auth_acl_check(void* userdata, const char* clientid, const char* username, const char* topic, int access)
#endif
{
	int granted = MOSQ_ERR_SUCCESS;
	return (granted);

}

