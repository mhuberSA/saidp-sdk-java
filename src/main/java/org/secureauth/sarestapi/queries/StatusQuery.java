package org.secureauth.sarestapi.queries;

import org.secureauth.sarestapi.resources.Resource;

/**
 *
 */
public class StatusQuery {

	public static String queryStatus(String realm, String userName){
		return realm + Resource.APPLIANCE_USERS + userName + Resource.APPLIANCE_STATUS;
	}
}
