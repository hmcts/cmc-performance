package uk.gov.hmcts.reform.cmc.performance.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import uk.gov.hmcts.reform.cmc.performance.scenarios._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{EmailNotification, Environment}

class CMCSimulation extends Simulation {
  
  val BaseURL = Environment.baseURL
  val loginFeeder = csv("login.csv").circular
  val defendantloginFeeder = csv("defendantlogin.csv").circular
  val claimNumbersFeeder = csv("defendantloginclaimnumbers.csv").circular
  val claimcreatedefuserFeeder = csv("claimcreatedeflogin.csv").circular
  val defendantdetailsFeed=csv("defendantdetails.csv").circular
  val httpProtocol = Environment.HttpProtocol
    .baseUrl(BaseURL)
    //.doNotTrackHeader("1")
    .inferHtmlResources()
    .silentResources
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-GB,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0")
  
  
  // below scenario is for user data creation
  val UserCreationScenario = scenario("CMC User Creation")
    .exec(
      CreateUser.CreateCitizen("citizen")
        .pause(20)
    )
  
  //below scenario is to generate claims data
  
  val CMCClaimsTS = scenario("CMC Claims Testing Support")
    .feed(claimcreatedefuserFeeder).feed(loginFeeder)
    //.repeat("${repeatcount}"){
    .repeat(1){
      exec(CMC_Claimant_TestingSupport.home)
        .exec(CMC_Claimant_TestingSupport.login)
        .exec(CMC_Claimant_TestingSupport.testingSupport)
        .exec(CMC_Claimant_TestingSupport.testingSupportdraftget)
        .exec(CMC_Claimant_TestingSupport.testingSupportdraftpost)
        .exec(CMC_Claimant_TestingSupport.testingsupportdefemail)
        .exec(CMC_Claimant_TestingSupport.testingsupportdefemailpost)
        .exec(CMC_Claimant_TestingSupport.testingsupportdefmobile)
        .exec(CMC_Claimant_TestingSupport.testingsupportdefmobilepost)
        .exec(CMC_Claimant_TestingSupport.checkAndSend)
        .exec(EmailNotification.getPin)
        .exec(CMC_Claimant.cmcLogout)
    }
  
  val CMCClaims = scenario("CMC Claims")
    .feed(defendantloginFeeder).feed (loginFeeder)
    .repeat(2) {
      exec (CMC_Claimant.home)
        .exec (CMC_Claimant.login)
        .exec (CMC_Claimant.eligibility)
        .exec (CMC_Claimant.resolvingDispute)
        .exec (CMC_Claimant.completingClaim)
        .exec (CMC_Claimant.yourDetails)
        .exec (CMC_Claimant.TheirDetails)
        .exec (CMC_Claimant.amount)
        .exec (CMC_Claimant.reason)
        .exec (CMC_Claimant.checkAndSend)
        .exec (EmailNotification.getPin)
        .exec(CMCDefendant.landingPage)
         .exec(CMCDefendant.startPage)
         .exec(CMCDefendant.claimNumber)
         //.exec(CMC_Defendant.enterpinGet)
         .exec(CMCDefendant.enterpinPost)
         .exec(CMCDefendant.ClaimSummary)
        .exec (CMC_Claimant.cmcLogout)
    }
  
  //below scenario is to link the claims to defendants for datagen
  
  val CMC_Link_Defendant=scenario("Claims Linking To Defendants")
    .feed(defendantdetailsFeed)
    .exec(CMCDefendant.landingPage)
    .exec(CMCDefendant.startPage)
    .exec(CMCDefendant.claimNumber)
    .exec(CMCDefendant.enterpinPost)
    .exec(CMCDefendant.ClaimSummary)
    .exec(CMCDefendant.loginAsDefendantGet)
    .exec(CMCDefendant.loginAsDefendant)
    .exec(CMCDefendant.cmcdefLogout)
  
  
  //below scenario is for OCMC testing the dashboard with defendants having more claims
  val CMC_Defendant_Response=scenario("CMC Defendants Response")
    .feed(defendantloginFeeder)
    .exec(CMCDefendant.dashboard)
    .exec(CMCDefendant.defendantlogin)
    .exec(CMCDefendant.casetaskList)
    .exec(CMCDefendant.yourdetailsconfirm)
    .exec(CMCDefendant.defendantDetails)
    .exec(CMCDefendant.dob)
    .exec(CMCDefendant.mobile)
    .exec(CMCDefendant.moreTimeRequest)
    .exec(CMCDefendant.responseType)
    .exec(CMCDefendant.reject_All_Claims)
    .exec(CMCDefendant.yourDefence)
    .exec(CMCDefendant.timeLine)
    .exec(CMCDefendant.evidence)
    .exec(CMCDefendant.freeMediation)
    .exec(CMCDefendant.freeMediationDecision)
    .exec(CMCDefendant.freeMediationAgreement)
    .exec(CMCDefendant.mediationusage)
    .exec(CMCDefendant.supportrequired)
    .exec(CMCDefendant.hearinglocation)
    .exec(CMCDefendant.selfwitness)
    .exec(CMCDefendant.otherwitness)
    .exec(CMCDefendant.hearingdates)
    //.exec(CMCDefendant.checkAndSend)
    .exec(CMCDefendant.dashboard)
    .exec(CMCDefendant.cmcdefLogout)
  
  val CMC_Defendant_Session=scenario("CMC Defendants Claim Numbers")
    .feed(claimNumbersFeeder)
    .exec(ClaimNumber.getIdamAuthCode)
    .exec(ClaimNumber.getClaimNumber)
    
  setUp(
    CMC_Defendant_Response.inject(nothingFor(1),rampUsers(50) during (1200)),
      CMC_Link_Defendant.inject(nothingFor(50),rampUsers(300) during (1200))
  ).protocols(httpProtocol)
  
 /* setUp(
    CMC_Defendant_Response.inject(nothingFor(1),rampUsers(1) during (1)),
    CMC_Link_Defendant.inject(nothingFor(50),rampUsers(1) during (1))
  ).protocols(httpProtocol)*/
  
  // below setup is to create bulk claims for the defendants
  
  /*setUp(
    CMC_Defendant_Session.inject(nothingFor(1),rampUsers(20) during (600))
  ).protocols(httpProtocol)*/
  
  /*setUp(
    CMCClaimsTS.inject(nothingFor(1),rampUsers(340) during (2700))
  ).protocols(httpProtocol)*/
}