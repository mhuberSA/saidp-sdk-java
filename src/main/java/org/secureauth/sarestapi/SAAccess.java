package org.secureauth.sarestapi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.secureauth.sarestapi.data.*;
import org.secureauth.sarestapi.data.BehavioralBio.BehaveBioRequest;
import org.secureauth.sarestapi.data.DFP.DFP;
import org.secureauth.sarestapi.data.NumberProfile.CarrierInfo;
import org.secureauth.sarestapi.data.Requests.BehaveBioResetRequest;
import org.secureauth.sarestapi.data.Response.BehaveBioResponse;
import org.secureauth.sarestapi.data.Requests.*;
import org.secureauth.sarestapi.data.Response.*;
import org.secureauth.sarestapi.data.Requests.UserPasswordRequest;
import org.secureauth.sarestapi.data.Response.UserProfileResponse;
import org.secureauth.sarestapi.data.UserProfile.NewUserProfile;
import org.secureauth.sarestapi.data.UserProfile.UserProfile;
import org.secureauth.sarestapi.data.UserProfile.UserToGroups;
import org.secureauth.sarestapi.data.UserProfile.UsersToGroup;
import org.secureauth.sarestapi.exception.SARestAPIException;
import org.secureauth.sarestapi.queries.*;
import org.secureauth.sarestapi.resources.Resource;
import org.secureauth.sarestapi.resources.SAExecuter;
import org.secureauth.sarestapi.util.JSONUtil;
import org.secureauth.sarestapi.util.RestApiHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rrowcliffe@secureauth.com
 * <p>
 *     SAAccess is a class that allows access to the SecureAuth REST API. The intention is to provide an easy method to access
 *     the Secureauth Authentication Rest Services.
 * </p>
 */

public class SAAccess implements ISAAccess{

    private static Logger logger = LoggerFactory.getLogger(SAAccess.class);
    protected SABaseURL saBaseURL;
    protected SAAuth saAuth;
    protected SAExecuter saExecuter;

    /**
     *<p>
     *     Returns a SAAccess Object that can be used to query the SecureAuth Rest API
     *     This should be the default object used when setting up connectivity to the SecureAuth Appliance
     *</p>
     * @param host FQDN of the SecureAuth Appliance
     * @param port The port used to access the web application on the Appliance.
     * @param ssl Use SSL
     * @param realm the Configured Realm that enables the RESTApi
     * @param applicationID The Application ID from the Configured Realm
     * @param applicationKey The Application Key from the Configured Realm
     *
     * @deprecated from 1.0.6.0, replace by {@link org.secureauth.sarestapi.util.SAFactory}
     */
    @Deprecated
    public SAAccess(String host, String port,boolean ssl, String realm, String applicationID, String applicationKey){
        saBaseURL=new SABaseURL(host,port,ssl);
        saAuth = new SAAuth(applicationID,applicationKey,realm);
        saExecuter=new SAExecuter(saBaseURL);
    }

    /**
     *<p>
     *     Returns a SAAccess Object that can be used to query the SecureAuth Rest API
     *     This should be the default object used when setting up connectivity to the SecureAuth Appliance
     *     This Object will allow users to support selfSigned Certificates
     *</p>
     * @param host FQDN of the SecureAuth Appliance
     * @param port The port used to access the web application on the Appliance.
     * @param ssl Use SSL
     * @param selfSigned  Support for SeflSigned Certificates. Setting to enable disable self signed cert support
     * @param realm the Configured Realm that enables the RESTApi
     * @param applicationID The Application ID from the Configured Realm
     * @param applicationKey The Application Key from the Configured Realm
     *
     * @deprecated from 1.0.6.0, replace by {@link org.secureauth.sarestapi.util.SAFactory}
     */
    @Deprecated
    public SAAccess(String host, String port,boolean ssl,boolean selfSigned, String realm, String applicationID, String applicationKey){
        saBaseURL=new SABaseURL(host,port,ssl,selfSigned);
        saAuth = new SAAuth(applicationID,applicationKey,realm);
        saExecuter=new SAExecuter(saBaseURL);
    }

    /**
     *<p>
     *     Returns a SAAccess Object that can be used to query the SecureAuth Rest API
     *     This should be the default object used when setting up connectivity to the SecureAuth Appliance
     *     This Object will allow users to support selfSigned Certificates
     *</p>
     * @param saBaseURL {@link org.secureauth.sarestapi.data.SABaseURL}
     * @param saAuth {@link org.secureauth.sarestapi.data.SAAuth}
     * @param saExecuter {@link org.secureauth.sarestapi.resources.SAExecuter}
     */
    public SAAccess(SABaseURL saBaseURL, SAAuth saAuth, SAExecuter saExecuter){
        this.saBaseURL= saBaseURL;
        this.saAuth = saAuth;
        this.saExecuter = saExecuter;
    }

    /**
     * <p>
     *     Returns IP Risk Evaluation from the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param ipAddress The IP Address of the user making the request for access
     * @return {@link org.secureauth.sarestapi.data.IPEval}
     *
     */
    public IPEval iPEvaluation(String userId, String ipAddress){
        String ts = getServerTime();
        IPEvalRequest ipEvalRequest =new IPEvalRequest();
        ipEvalRequest.setIp_address(ipAddress);
        ipEvalRequest.setUser_id(userId);
        ipEvalRequest.setType("risk");

        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", IPEvalQuery.queryIPEval(saAuth.getRealm()), ipEvalRequest, ts);

        try{

            return saExecuter.executeIPEval(header,saBaseURL.getApplianceURL() + IPEvalQuery.queryIPEval(saAuth.getRealm()),ipEvalRequest,ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Returns the list of Factors available for the specified user
     * </p>
     * @param userId the userid of the identity you wish to have a list of possible second factors
     * @return {@link FactorsResponse}
     */
    public FactorsResponse factorsByUser(String userId){
        String ts = getServerTime();
        String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_GET, FactorsQuery.queryFactors(saAuth.getRealm(), userId), ts);

        try{
            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + FactorsQuery.queryFactors(saAuth.getRealm(), userId), ts, FactorsResponse.class);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send push to accept request asynchronously
     * </p>
     *
     * @param userId  the user id of the identity
     * @param factorId the P2A Id to be compared against
     * @param endUserIP The End Users IP Address
     * @param clientCompany The Client Company Name
     * @param clientDescription The Client Description
     * @return {@link FactorsResponse}
     */
    public ResponseObject sendPushToAcceptReq(String userId, String factorId, String endUserIP, String clientCompany, String clientDescription){
       return sendPushReq(userId, factorId, endUserIP, clientCompany, clientDescription, "push_accept");
    }

    public ResponseObject sendPushToAcceptSymbolReq(String userId, String factorId, String endUserIP, String clientCompany, String clientDescription){
        return sendPushReq(userId, factorId, endUserIP, clientCompany, clientDescription, "push_accept_symbol");
    }

    private ResponseObject sendPushReq(String userid, String factor_id, String endUserIP, String clientCompany, String clientDescription, String type) {
        String ts = getServerTime();
        PushToAcceptRequest req = new PushToAcceptRequest();
        req.setUser_id(userid);
        req.setType(type);
        req.setFactor_id(factor_id);
        PushAcceptDetails pad = new PushAcceptDetails();
        pad.setEnduser_ip(endUserIP);
        if (clientCompany != null) {
            pad.setCompany_name(clientCompany);
        }
        if (clientDescription != null) {
            pad.setApplication_description(clientDescription);
        }
        req.setPush_accept_details(pad);
        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), req,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), req,ts, ResponseObject.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send push to accept biometric request asynchronously
     * </p>
     *
     * @param biometricType fingerprint, face
     * @param userId  the user id of the identity
     * @param factorId the P2A Id to be compared against
     * @param endUserIP The End Users IP Address
     * @param clientCompany The Client Company Name
     * @param clientDescription The Client Description
     * @return {@link FactorsResponse}
     */
    public ResponseObject sendPushBiometricReq(String biometricType, String userId, String factorId, String endUserIP, String clientCompany, String clientDescription) {
        String ts = getServerTime();
        PushToAcceptBiometricsRequest req = new PushToAcceptBiometricsRequest();
        req.setUser_id(userId);
        req.setType("push_accept_biometric");
        req.setFactor_id(factorId);
        req.setBiometricType( biometricType );
        PushAcceptDetails pad = new PushAcceptDetails();
        pad.setEnduser_ip(endUserIP);
        if (clientCompany != null) {
            pad.setCompany_name(clientCompany);
        }

        if (clientDescription != null) {
            pad.setApplication_description(clientDescription);
        }

        req.setPush_accept_details(pad);
        String header = RestApiHeader.getAuthorizationHeader(this.saAuth, "POST", AuthQuery.queryAuth(this.saAuth.getRealm()), req, ts);

        try {
            return (ResponseObject)this.saExecuter.executePostRequest(header, this.saBaseURL.getApplianceURL() + AuthQuery.queryAuth(this.saAuth.getRealm()), req, ts, ResponseObject.class);
        } catch (Exception var12) {
            logger.error("Exception occurred executing REST query::\n" + var12.getMessage() + "\n", var12);
            return null;
        }
    }

    /**
     * <p>
     *     Perform adaptive auth query
     * </p>
     * @param userId the user id of the identity
     * @param endUserIP the IP of requesting client
     * @return {@link FactorsResponse}
     */
    public AdaptiveAuthResponse adaptiveAuthQuery(String userId, String endUserIP){
        String ts = getServerTime();
        AdaptiveAuthRequest req = new AdaptiveAuthRequest(userId, endUserIP);
        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAAuth(saAuth.getRealm()), req,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAAuth(saAuth.getRealm()), req, ts, AdaptiveAuthResponse.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    public PushAcceptStatus queryPushAcceptStatus(String refId){
        String ts = getServerTime();
        String getUri = AuthQuery.queryAuth(saAuth.getRealm()) + "/" + refId;
        String header = RestApiHeader.getAuthorizationHeader(saAuth,"GET", getUri,ts);

        try{
            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + getUri,ts, PushAcceptStatus.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }



    /**
     *
     * <p>
     *     Checks if the Username exists within the datastore within SecureAuth
     * </p>
     * @param userId the userid of the identity
     * @return {@link ResponseObject}
     */
    public BaseResponse validateUser(String userId){
        String ts = getServerTime();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("user_id");

        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);


        try{
            return saExecuter.executeValidateUser(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * the OTP throttling count to 0 after the end-user successfully authenticates;
     * the attempt count is stored in a directory attribute configured in the Web Admin
     * @param userId id of user
     * @return base answer
     */
    public ThrottleResponse resetThrottleReq(String userId){
        try{
            String ts = getServerTime();
            AuthRequest authRequest = new AuthRequest();
            ThrottleRequest throttleRequest = new ThrottleRequest(0);

            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_PUT, ThrottleQuery.queryThrottles(saAuth.getRealm(), userId), throttleRequest, ts);

            return saExecuter.executePutRequest(header,saBaseURL.getApplianceURL() + ThrottleQuery.queryThrottles(saAuth.getRealm(), userId), throttleRequest,ThrottleResponse.class, ts);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query:\n" + e.getMessage());
        }
    }

    /**
     * GET the end-user's current count of OTP usage attempts
     * @param userId id of user
     * @return base answer
     */
    public ThrottleResponse getThrottleReq(String userId){
        try{
            String ts = getServerTime();
            AuthRequest authRequest = new AuthRequest();

            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_GET, ThrottleQuery.queryThrottles(saAuth.getRealm(), userId), ts);

            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + ThrottleQuery.queryThrottles(saAuth.getRealm(), userId), ts, ThrottleResponse.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query:\n" + e.getMessage());
        }
    }

    /**
     * <p>
     *     Checks the users password against SecureAuth Datastore
     * </p>
     * @param userId the userid of the identity
     * @param password The password of the user to validate
     * @return {@link ResponseObject}
     */
    public BaseResponse validateUserPassword(String userId, String password){
        String ts = getServerTime();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("password");
        authRequest.setToken(password);

        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateUserPassword(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Checks the users pin against SecureAuth Datastore
     * </p>
     * @param userId the userid of the identity
     * @param pin The pin of the user to validate
     * @return {@link ResponseObject}
     */
    public BaseResponse validateUserPin(String userId, String pin){
        String ts = getServerTime();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("pin");
        authRequest.setToken(pin);

        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateUserPin(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Validate the users Answer to a KB Question
     * </p>
     * @param userId the userid of the identity
     * @param answer The answer to the KBA
     * @param factorId the KB Id to be compared against
     * @return {@link ResponseObject}
     */
    public BaseResponse validateKba(String userId, String answer, String factorId){
        String ts = getServerTime();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("kba");
        authRequest.setToken(answer);
        authRequest.setFactor_id(factorId);

        String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateKba(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     *<p>
     *     Validate the Oath Token
     *</p>
     * @param userId the userid of the identity
     * @param otp The One Time Passcode to validate
     * @param factorId The Device Identifier
     * @return {@link ResponseObject}
     */
    public BaseResponse validateOath(String userId, String otp, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("oath");
        authRequest.setToken(otp);
        authRequest.setFactor_id(factorId);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateOath(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Phone
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Phone Property   "Phone1"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverOTPByPhone(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("call");
        authRequest.setFactor_id(factorId);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByPhone(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Phone Ad Hoc
     * </p>
     * @param userId the userid of the identity
     * @param phoneNumber  Phone Number to call
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverAdHocOTPByPhone(String userId, String phoneNumber){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("call");
        authRequest.setToken(phoneNumber);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByPhone(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }


    /**
     * <p>
     *     Send One Time Passcode by SMS to Registered User
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Phone Property   "Phone1"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverOTPBySMS(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("sms");
        authRequest.setFactor_id(factorId);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPBySMS(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }


    /**
     * <p>
     *     Validate One Time Passcode sent by SMS
     * </p>
     * @param userId the userid of the identity
     * @param otp  OTP Value to compare against what was sent
     * @return {@link ValidateOTPResponse}
     */
    public ValidateOTPResponse validateOTP(String userId, String otp){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        ValidateOTPRequest validateOTPRequest = new ValidateOTPRequest();

        validateOTPRequest.setUser_id(userId);
        validateOTPRequest.setOtp(otp);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", ValidateOTPQuery.queryValidateOTP(saAuth.getRealm()), validateOTPRequest,ts);

        try{
            return saExecuter.executeValidateOTP(header,saBaseURL.getApplianceURL() + ValidateOTPQuery.queryValidateOTP(saAuth.getRealm()),validateOTPRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by SMS Ad Hoc
     * </p>
     * @param userId the userid of the identity
     * @param phoneNumber  Phone Number to send SMS to
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverAdHocOTPBySMS(String userId, String phoneNumber){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("sms");
        authRequest.setToken(phoneNumber);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPBySMS(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Email to Help Desk
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Help Desk Property   "HelpDesk1"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverHelpDeskOTPByEmail(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("help_desk");
        authRequest.setFactor_id(factorId);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByEmail(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Email
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Email Property   "Email1"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverOTPByEmail(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("email");
        authRequest.setFactor_id(factorId);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByEmail(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Email Ad Hoc
     * </p>
     * @param userId the userid of the identity
     * @param emailAddress  Email Address
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverAdHocOTPByEmail(String userId, String emailAddress){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("email");
        authRequest.setToken(emailAddress);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByEmail(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }


    /**
     * <p>
     *     Send One Time Passcode by Push
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Device Property   "z0y9x87wv6u5t43srq2p1on"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverOTPByPush(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("push");
        authRequest.setFactor_id(factorId);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts, ResponseObject.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Helpdesk
     * </p>
     * @param userId the userid of the identity
     * @param factorId  Help Desk Property   "HelpDesk1"
     * @return {@link ResponseObject}
     */
    public ResponseObject deliverOTPByHelpDesk(String userId, String factorId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userId);
        authRequest.setType("help_desk");
        authRequest.setFactor_id(factorId);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByHelpDesk(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Returns response to Access History Post Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param ipAddress The IP Address of the user to be stored in the Datastore for use when evaluating Geo-Velocity
     * @return {@link AccessHistoryRequest}
     *
     */
    public ResponseObject accessHistory(String userId, String ipAddress){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        AccessHistoryRequest accessHistoryRequest =new AccessHistoryRequest();
        accessHistoryRequest.setIp_address(ipAddress);
        accessHistoryRequest.setUser_id(userId);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AccessHistoryQuery.queryAccessHistory(saAuth.getRealm()), accessHistoryRequest, ts);

        try{

            return saExecuter.executeAccessHistory(header,saBaseURL.getApplianceURL() + AccessHistoryQuery.queryAccessHistory(saAuth.getRealm()),accessHistoryRequest,ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Confirm the DFP data from Client using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param fingerprintId The ID of the finger print to check against the data store
     * @return {@link DFPConfirmResponse}
     *
     */
    public DFPConfirmResponse DFPConfirm(String userId, String fingerprintId){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        DFPConfirmRequest dfpConfirmRequest =new DFPConfirmRequest();
        dfpConfirmRequest.setUser_id(userId);
        dfpConfirmRequest.setFingerprint_id(fingerprintId);


        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", DFPQuery.queryDFPConfirm(saAuth.getRealm()), dfpConfirmRequest, ts);

        try{

            return saExecuter.executeDFPConfirm(header,saBaseURL.getApplianceURL() + DFPQuery.queryDFPConfirm(saAuth.getRealm()), dfpConfirmRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Validate the DFP data from Client using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param hostAddress The ID of the finger print to check against the data store
     * @param jsonString The JSON String provided by the Java Script
     * @return {@link DFPValidateResponse}
     *
     */
    public DFPValidateResponse DFPValidateNewFingerprint(String userId, String hostAddress, String jsonString){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        DFPValidateRequest dfpValidateRequest = new DFPValidateRequest();
        DFP dfp = JSONUtil.getDFPFromJSONString(jsonString);
        dfpValidateRequest.setFingerprint(dfp);
        dfpValidateRequest.setUser_id(userId);
        dfpValidateRequest.setHost_address(hostAddress);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", DFPQuery.queryDFPValidate(saAuth.getRealm()), dfpValidateRequest, ts);

        try{

            return saExecuter.executeDFPValidate(header,saBaseURL.getApplianceURL() + DFPQuery.queryDFPValidate(saAuth.getRealm()), dfpValidateRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Returns the url for the JavaScript Source for DFP
     * </p>
     * @return {@link JSObjectResponse}
     */
    public JSObjectResponse javaScriptSrc(){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"GET",DFPQuery.queryDFPjs(saAuth.getRealm()),ts);


        try{
            return saExecuter.executeGetJSObject(header,saBaseURL.getApplianceURL() + DFPQuery.queryDFPjs(saAuth.getRealm()),ts, JSObjectResponse.class);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * Start of Behavior Bio Metrics Methods
     *
     */


    /**
     * <p>
     *     Returns the url for the JavaScript Source for BehaveBioMetrics
     * </p>
     * @return {@link JSObjectResponse}
     */
    public JSObjectResponse BehaveBioJSSrc(){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"GET",BehaveBioQuery.queryBehaveBiojs(saAuth.getRealm()),ts);


        try{
            return saExecuter.executeGetJSObject(header,saBaseURL.getApplianceURL() + BehaveBioQuery.queryBehaveBiojs(saAuth.getRealm()),ts, JSObjectResponse.class);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }


    /**
     * <p>
     *     Submit Behave Bio Profile using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param behaviorProfile The Behavioral Profile of the user
     * @param hostAddress The IP Address of the user
     * @param userAgent  The Browser User Agent of the user
     *
     * @return {@link BehaveBioResponse}
     *
     */
    public BehaveBioResponse BehaveBioProfileSubmit(String userId, String behaviorProfile, String hostAddress, String userAgent){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        BehaveBioRequest behaveBioRequest = new BehaveBioRequest();
        behaveBioRequest.setUserId(userId);
        behaveBioRequest.setBehaviorProfile(behaviorProfile);
        behaveBioRequest.setHostAddress(hostAddress);
        behaveBioRequest.setUserAgent(userAgent);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", BehaveBioQuery.queryBehaveBio(saAuth.getRealm()), behaveBioRequest, ts);

        try{

            return saExecuter.executeBehaveBioPost(header,saBaseURL.getApplianceURL() + BehaveBioQuery.queryBehaveBio(saAuth.getRealm()), behaveBioRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Submit Reset Request to Behave Bio Profile using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param fieldName The Behavioral FieldName to Reset
     * @param fieldType The Behavioral FieldType to Reset
     * @param deviceType  The Behavioral DeviceType to Reset
     *
     * @return {@link ResponseObject}
     *
     */
    public ResponseObject BehaveBioProfileReset(String userId, String fieldName, String fieldType, String deviceType){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        BehaveBioResetRequest behaveBioResetRequest = new BehaveBioResetRequest();
        behaveBioResetRequest.setUserId(userId);
        behaveBioResetRequest.setFieldName(fieldName);
        behaveBioResetRequest.setFieldType(fieldType);
        behaveBioResetRequest.setDeviceType(deviceType);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"PUT", BehaveBioQuery.queryBehaveBio(saAuth.getRealm()), behaveBioResetRequest, ts);

        try{

            return saExecuter.executeBehaveBioReset(header,saBaseURL.getApplianceURL() + BehaveBioQuery.queryBehaveBio(saAuth.getRealm()), behaveBioResetRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * END of Behavior Bio Metrics Methods
     *
     */

    /**
     * Start of IDM Methods
     */

    /**
     * <p>
     *     Creates User / Profile
     * </p>
     * @param newUserProfile The newUserProfile Object
     * @return {@link ResponseObject}
     */
    public ResponseObject createUser(NewUserProfile newUserProfile){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST",IDMQueries.queryUsers(saAuth.getRealm()),newUserProfile,ts);

        /*
        At a minimum creating a user requires UserId and Passowrd
         */
        if(newUserProfile.getUserId() != null && !newUserProfile.getUserId().isEmpty() && newUserProfile.getPassword() != null && !newUserProfile.getPassword().isEmpty()){
            try{
                return saExecuter.executeUserProfileCreateRequest(header,saBaseURL.getApplianceURL() + IDMQueries.queryUsers(saAuth.getRealm()),newUserProfile,ts,ResponseObject.class);

            }catch (Exception e){
                logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
            }
        }
        return null;
    }

    /**
     * <p>
     *     Update User / Profile
     * </p>
     * @param userId the UserID tied to the Profile Object
     * @param userProfile The User'sProfile Object to be updated
     * @return {@link ResponseObject}
     */
    public ResponseObject updateUser(String userId, NewUserProfile userProfile){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"PUT",IDMQueries.queryUserProfile(saAuth.getRealm(),userId),userProfile,ts);


        try{
            return saExecuter.executeUserProfileUpdateRequest(header,
                    saBaseURL.getApplianceURL() + IDMQueries.queryUserProfile(saAuth.getRealm(),userId),
                    userProfile,
                    ts,
                    ResponseObject.class);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Associate User to Group
     * </p>
     * @param userId the user id of the identity
     * @param groupName The Name of the group to associate the user to
     * @return {@link GroupAssociationResponse}
     */
    public ResponseObject addUserToGroup(String userId, String groupName){
        try{
            String ts = getServerTime();
            String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", IDMQueries.queryUserToGroup(saAuth.getRealm(),userId,groupName),ts);

            return saExecuter.executeSingleUserToSingleGroup(header,saBaseURL.getApplianceURL() + IDMQueries.queryUserToGroup(saAuth.getRealm(),userId,groupName), ts, ResponseObject.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query:\n" + e.getMessage() + "\n", e);
        }
    }

    /**
     * <p>
     *     Associate Group to Users
     * </p>
     * @param usersToGroup The Users to Group object holding the userIds
     * @param groupName The Name of the group to associate the user to
     * @return {@link GroupAssociationResponse}
     */
    public GroupAssociationResponse addUsersToGroup(UsersToGroup usersToGroup, String groupName){
        try{
            String ts = getServerTime();
            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_POST, IDMQueries.queryGroupToUsers(saAuth.getRealm(),groupName),usersToGroup,ts);

            return saExecuter.executeGroupToUsersRequest(header,saBaseURL.getApplianceURL() + IDMQueries.queryGroupToUsers(saAuth.getRealm(),groupName), usersToGroup, ts, GroupAssociationResponse.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query::\n" + e.getMessage() + "\n", e);
        }
    }


    /**
     * <p>
     *     Associate Group to User
     * </p>
     * @param groupName the Group Name
     * @param userId The userId to associate to the group
     * @return {@link GroupAssociationResponse}
     */
    public GroupAssociationResponse addGroupToUser(String groupName, String userId){
        try{
            String ts = getServerTime();

            String header = RestApiHeader.getAuthorizationHeader(saAuth,"POST", IDMQueries.queryGroupToUser(saAuth.getRealm(),userId,groupName),ts);

            return saExecuter.executeSingleGroupToSingleUser(header,saBaseURL.getApplianceURL() + IDMQueries.queryGroupToUser(saAuth.getRealm(),userId,groupName), ts, GroupAssociationResponse.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query:\n" + e.getMessage() + "\n", e);
        }
    }

    /**
     * <p>
     *     Associate User To Groups
     * </p>
     * @param userId The UserId we are going to assign Groups to
     * @param userToGroups The UserToGroups Object holding the list of groups to associate to the user
     * @return {@link GroupAssociationResponse}
     */
    public GroupAssociationResponse addUserToGroups(String userId, UserToGroups userToGroups){
        try{
            String ts = getServerTime();
            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_POST, IDMQueries.queryUserToGroups(saAuth.getRealm(),userId),userToGroups,ts);

            return saExecuter.executeUserToGroupsRequest(header,saBaseURL.getApplianceURL() + IDMQueries.queryUserToGroups(saAuth.getRealm(),userId), userToGroups, ts, GroupAssociationResponse.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing REST query:\n" + e.getMessage() + "\n", e);
        }
    }

    /**
     * <p>
     *     Returns the UserProfile for the specified user
     * </p>
     * @param userId the userid of the identity you wish to have a list of possible second factors
     * @return {@link UserProfileResponse}
     */
    public UserProfileResponse getUserProfile(String userId){
        String ts = getServerTime();
        String header = RestApiHeader.getAuthorizationHeader(saAuth,"GET",IDMQueries.queryUserProfile(saAuth.getRealm(), userId),ts);

        try{
            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + IDMQueries.queryUserProfile(saAuth.getRealm(), userId),ts, UserProfileResponse.class);

        }catch (Exception e){
            logger.error("Exception occurred executing REST query:\n" + e.getMessage() + "\n");
        }
        return null;
    }

    /**
     * <p>
     *     Administrative Password Reset for the specified user
     * </p>
     * @param userId the userid of the identity you wish to have a list of possible second factors
     * @param password the users new password
     * @return {@link ResponseObject}
     */
    public ResponseObject passwordReset(String userId, String password){
        String ts = getServerTime();
        UserPasswordRequest userPasswordRequest = new UserPasswordRequest();
        userPasswordRequest.setPassword(password);
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST",IDMQueries.queryUserResetPwd(saAuth.getRealm(), userId),userPasswordRequest,ts);


        try{
            return saExecuter.executeUserPasswordReset(header,saBaseURL.getApplianceURL() + IDMQueries.queryUserResetPwd(saAuth.getRealm(), userId),userPasswordRequest,ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Self Service Password Reset for the specified user
     * </p>
     * @param userId the userid of the identity you wish to have a list of possible second factors
     * @param currentPassword the users Current password
     * @param newPassword the users new Password
     * @return {@link ResponseObject}
     */
    public ResponseObject passwordChange(String userId, String currentPassword, String newPassword){
        String ts = getServerTime();
        UserPasswordRequest userPasswordRequest = new UserPasswordRequest();
        userPasswordRequest.setCurrentPassword(currentPassword);
        userPasswordRequest.setNewPassword(newPassword);
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST",IDMQueries.queryUserChangePwd(saAuth.getRealm(), userId),userPasswordRequest,ts);


        try{
            return saExecuter.executeUserPasswordChange(header,saBaseURL.getApplianceURL() + IDMQueries.queryUserChangePwd(saAuth.getRealm(), userId),userPasswordRequest,ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * End of IDM Methods
     */

    /**
     * Start of  Phone Number Profile Methods
     */

    /**
     * <p>
     *     Submit User Name and Phone Number to the Phone Number Profiling service using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param phoneNumber The Phone number to get a profile on
     *
     * @return {@link NumberProfileResponse}
     *
     */
    public NumberProfileResponse PhoneNumberProfileSubmit(String userId, String phoneNumber){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        NumberProfileRequest numberProfileRequest = new NumberProfileRequest();
        numberProfileRequest.setUser_id(userId);
        numberProfileRequest.setPhone_number(phoneNumber);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", NumberProfileQuery.queryNumberProfile(saAuth.getRealm()), numberProfileRequest, ts);

        try{

            return saExecuter.executeNumberProfilePost(header,saBaseURL.getApplianceURL() + NumberProfileQuery.queryNumberProfile(saAuth.getRealm()), numberProfileRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Submit Update to Phone Number Profiling Service using the Rest API
     * </p>
     * @param userId The User ID that you want to validate from
     * @param phoneNumber user phone number provided
     * @param portedStatus user phone status for the option to block phone numbers that recently changed carriers (not_ported, ported)
     * @param carrierCode 6-digit number or a concatenation of the country code and phone type
     * @param carrier name of the carrier or a concatenation of the country code and phone type
     * @param countryCode  2-character country code
     * @param networkType phone connection source (landline, tollfree, mobile, virtual, unknown, landline_tollfree)
     *
     * @return {@link BaseResponse}
     *
     */
    public BaseResponse UpdatePhoneNumberProfile(String userId, String phoneNumber, String portedStatus, String carrierCode, String carrier, String countryCode, String networkType){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        NumberProfileUpdateRequest numberProfileUpdateRequest = new NumberProfileUpdateRequest();
        numberProfileUpdateRequest.setUser_id(userId);
        numberProfileUpdateRequest.setPhone_number(phoneNumber);
        numberProfileUpdateRequest.setPortedStatus(portedStatus);
        CarrierInfo carrierInfo = new CarrierInfo();
        carrierInfo.setCarrierCode(carrierCode);
        carrierInfo.setCarrier(carrier);
        carrierInfo.setCountryCode(countryCode);
        carrierInfo.setNetworkType(networkType);
        numberProfileUpdateRequest.setCarrierInfo(carrierInfo);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"PUT", NumberProfileQuery.queryNumberProfile(saAuth.getRealm()), numberProfileUpdateRequest, ts);

        try{

            return saExecuter.executeNumberProfileUpdate(header,saBaseURL.getApplianceURL() + NumberProfileQuery.queryNumberProfile(saAuth.getRealm()), numberProfileUpdateRequest, ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * Retrieves the user's status from the username in the endpoint URL and returns a response.
     * @param userId The User ID that you want to validate
     * @return {@link BaseResponse}
     */
    public BaseResponse getUserStatus(String userId){
        try{
            String ts = getServerTime();

            String query = StatusQuery.queryStatus(saAuth.getRealm(), userId);

            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_GET, query, ts);

            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + query, ts, BaseResponse.class);
        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing get user status query", e);
        }

    }

    /**
     * Method invokes a status to the user Id.
     * @param userId The User ID that you want to change status
     * @param status The new status [lock, unlock, enable, disable]
     * @return {@link BaseResponse}
     */
    public BaseResponse setUserStatus(String userId, String status){
        try{
            String ts = getServerTime();

            String query = StatusQuery.queryStatus(saAuth.getRealm(), userId);

            //payload
            StatusRequest statusRequestPayload = new StatusRequest(status);

            String header = RestApiHeader.getAuthorizationHeader(saAuth, Resource.METHOD_POST, query, statusRequestPayload, ts);

            return saExecuter.executePostRawRequest(header,saBaseURL.getApplianceURL() + query, statusRequestPayload, BaseResponse.class, ts);

        }catch (Exception e){
            throw new SARestAPIException("Exception occurred executing set user status query", e);
        }

    }

    /**
     * End of Number Profile Methods
     */

    /**
     * End of All SA Access methods
     */

    /**
     * Start Helper Methods
     * to fetch raw json
     * @param query url
     * @return raw response
     */
    public String executeGetRequest(String query) {
		String ts = getServerTime();
		RestApiHeader restApiHeader = new RestApiHeader();
		query = saAuth.getRealm() + query;
		String header = restApiHeader.getAuthorizationHeader(saAuth, "GET", query, ts);
		try {
			return saExecuter.executeRawGetRequest(header, saBaseURL.getApplianceURL() + query, ts);
		} catch (Exception e) {
			logger.error("Exception occurred executing REST query::\n" + e.getMessage() + "\n", e);
		}
		return null;
	}

    String getServerTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss.SSS z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    /**
     *
     * End Helper Methods
     */
}
