package uk.gov.hmcts.reform.cmc.performance.simulations

import io.gatling.core.Predef.{feed, _}
import io.gatling.core.scenario.Simulation
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{Common, EmailNotification, Environment}
import uk.gov.hmcts.reform.cmc.performance.scenarios.{CMC_Claimant, CMC_Claimant_TestingSupport, CreateUser}


class CMCSimulation extends Simulation {

  val BaseURL = Environment.baseURL
  val loginFeeder = csv("login.csv").circular
  val defendantloginFeeder = csv("defendantlogin.csv").circular
  val defendantdetailsFeed=csv("defendantdetails.csv").circular
  val httpProtocol = Environment.HttpProtocol
    .baseUrl(BaseURL)
    .doNotTrackHeader("1")
    .inferHtmlResources()
    .silentResources
  
  // below scenario is for user data creation
  val UserCreationScenario = scenario("CMC User Creation")
    .exec(
      CreateUser.CreateCitizen("citizen")
        .pause(20)
    )
  
  val CMCClaimsTS = scenario("CMC Claims Testing Support")
    .feed(defendantloginFeeder).feed(loginFeeder)
    .repeat(2) {
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
       /* .exec(CMC_Defendant.startPage)
        .exec(CMC_Defendant.claimNumber)
       // .exec(CMC_Defendant.enterpinGet)
        .exec(CMC_Defendant.enterpinPost)
        .exec(CMC_Defendant.ClaimSummary)*/
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
        //.exec(CMC_Defendant.landingPage)
       /* .exec(CMC_Defendant.startPage)
        .exec(CMC_Defendant.claimNumber)
        //.exec(CMC_Defendant.enterpinGet)
        .exec(CMC_Defendant.enterpinPost)
        .exec(CMC_Defendant.ClaimSummary)*/
        .exec (CMC_Claimant.cmcLogout)
    }
  /*val CMC_Defendant=scenario("CMC Defendants")
    .feed(defendantdetailsFeed)
    .exec(CMC_Defendant.landingPage)
      .exec(CMC_Defendant.startPage)
      .exec(CMC_Defendant.claimNumber)
      //.exec(CMC_Defendant.enterpinGet)
      .exec(CMC_Defendant.enterpinPost)
      .exec(CMC_Defendant.ClaimSummary)*/
  
   setUp(
     CMCClaimsTS.inject(nothingFor(1),rampUsers(3000) during (5400))
  ).protocols(httpProtocol)
}