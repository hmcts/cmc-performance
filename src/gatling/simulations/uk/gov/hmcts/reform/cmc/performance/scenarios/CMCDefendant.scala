package uk.gov.hmcts.reform.cmc.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CsrfCheck.{csrfParameter, csrfTemplate}
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CurrentPageCheck.currentPageTemplate
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment}

import scala.concurrent.duration._

object CMCDefendant {
  
  val BaseURL = Environment.baseURL
  val IdAMURL = Environment.idamURL
  val MinThinkTime = Environment.minThinkTime
  val MaxThinkTime = Environment.maxThinkTime
  val CommonHeader = Environment.commonHeader
  val paymentURL = Environment.PaymentURL
  val TotalAmount = scala.util.Random.nextInt(9999)
  
  
  val landingPage =
    group("TX01_CMC_Def_LandingPage_Get") {
      exec (http ("TX01_CMC_Def_LandingPage_Get")
        .get ("/first-contact/start")
        .check (CsrfCheck.save)
        .check (regex ("Start now")))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val startPage=
    group("TX02_CMC_Def_LandingPage_Post") {
      exec (http ("TX01_CMC_Def_LandingPage_Post")
        .post ("/first-contact/start")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("start-button", "Start now")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Enter your claim number"))
      )
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val claimNumber =
    group("TX03_CMC_Def_Login_ClaimNumber") {
      exec (http ("TX02_CMC_Def_Login_ClaimNumber")
        .post ("/first-contact/claim-reference")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("reference", "${claimno}")
        .check(css("input[name='redirect_uri']", "value").saveAs("redirectUri"))
        .check(css("input[name='client_id']", "value").saveAs("clientId"))
        .check(css("input[name='state']", "value").saveAs("state"))
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
        .check (regex ("Enter security code"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
   
  
  val enterpinGet =
    group("TX05_CMC_Def_PinGet") {
      exec (http ("TX01_CMC_Def_LandingPage_Get")
        .get (currentPageTemplate)
        .check (CsrfCheck.save)
        .check (CurrentPageCheck.save)
          .check(status.in(200,201)))
        
    }
      //  .pause(MinThinkTime seconds,MaxThinkTime seconds)
     /* val enterpinGet=
        group("TX05_CMC_Def_PinPost"){
        exec (http ("TX03_CMC_Def_Pin")
        .post (currentPageTemplate)
          .headers(Environment.headers_withpin)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("pinnumber", "${securitycode}")
        .formParam ("redirect_uri", "${redirectUri}")
        .formParam ("client_id", "${clientId}")
        .formParam ("state", "${state}")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)*/
  
  val enterpinPost=
    group("TX06_CMC_Def_PinPost"){
      exec (http ("TX06_CMC_Def_PinPost")
        .post (currentPageTemplate)
        .headers(Environment.headers_withpin)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("pinnumber","FNKEX3ME")
        .formParam ("redirect_uri","${redirectUri}")
        .formParam ("client_id","${clientId}")
        .formParam ("state","021MC984")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Claim details"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val ClaimSummary =
    group("TX06_CMC_Def_FirstContact_ClaimSummary") {
      exec (http ("TX04_CMC_Def_FirstContact_ClaimSummary")
        .post (currentPageTemplate)
          .headers(Environment.headers_claimsummary)
        .formParam (csrfParameter, csrfTemplate)
        .check (CurrentPageCheck.save)
       // .check (CsrfCheck.save)
        .check (regex ("Create an account or sign in"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
    
  val receiver_get =
    group("TX05_CMC_Def_Login_Receiver_Get") {
      exec (http ("TX05_CMC_Def_Login_Receiver_Get")
        .get (currentPageTemplate)
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Sign in"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val loginAsDefendant=
    group("TX06_CMC_Def_Login_As_Defendant") {
      exec (http ("TX06_CMC_Def_Login_As_Defendant")
        .post (currentPageTemplate)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("username", "cmcvv300@mailinator.com")
        .formParam ("password", "Pass19word")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save))
        .pause (MinThinkTime seconds, MaxThinkTime seconds)
    }
    
}
