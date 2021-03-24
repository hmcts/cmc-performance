package uk.gov.hmcts.reform.cmc.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CsrfCheck.{csrfParameter, csrfTemplate}
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.CurrentPageCheck.currentPageTemplate
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment}

import scala.concurrent.duration._

object CMCDefendant {
  
  val BaseURL = Environment.baseURL
  val baseDomain=Environment.baseDomain
  val IdAMURL = Environment.idamURL
  val MinThinkTime = Environment.minThinkTime
  val MaxThinkTime = Environment.maxThinkTime
  val CommonHeader = Environment.commonHeader
  val paymentURL = Environment.PaymentURL
  val TotalAmount = scala.util.Random.nextInt(9999)
  
  
  val landingPage =
    group("CMCDef_010_LandingPage_Get") {
      exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
        .exec (http ("LandingPage_Get")
        .get ("/first-contact/start")
          .headers(Environment.headers_firstcontact)
        .check (CsrfCheck.save)
        .check (regex ("Start now")))
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val startPage=
    group("CMCDef_020_LandingPage_Post") {
      exec (http ("LandingPage_Post")
        .post ("/first-contact/start")
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("start-button", "Start now")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Enter your claim number"))
      )
    }
    .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val claimNumber =
    group("CMCDef_030_ClaimNumber") {
      exec (http ("ClaimNumber")
        .post (currentPageTemplate)
        .headers(Environment.headers_25)
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
  
  val enterpinPost=
    group("CMCDef_040_PinPost"){
      exec (http ("Def_PinPost")
        .post (IdAMURL+"/loginWithPin")
       // .post (currentPageTemplate)
        .headers(Environment.headers_withpin)
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("pin","${pin}")
        .formParam ("redirect_uri","${redirectUri}")
        .formParam ("client_id","${clientId}")
        .formParam ("state","${state}")
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save)
        .check (regex ("Claim details"))
      )
    }
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  
  val ClaimSummary =
    group("CMCDef_050_ClaimSummary") {
      exec (http ("ClaimSummary")
        .post (currentPageTemplate)
          .headers(Environment.headers_25)
        .formParam (csrfParameter, csrfTemplate)
        .check (CurrentPageCheck.save)
       // .check (CsrfCheck.save)
        .check (regex ("Create an account or sign in"))
      )
    }
      
      .pause(MinThinkTime seconds,MaxThinkTime seconds)
  
  val loginAsDefendantGet =
    group("CMCDef_060_Login_As_DefendantGet") {
      exec (http ("Login_As_DefendantGet")
        .get (currentPageTemplate)
          .check(status.in(201,200,204))
        .check(regex("response_type=code&state=(.*)&client_id").saveAs("state"))
        .check(regex("&redirect_uri=(.*)&jwt=").saveAs("redirectURI"))
        .check(regex("&client_id=(.*)&redirect_uri").saveAs("clientId"))
        .check(regex("&scope=&jwt=(.*)\">Sign in to your account").saveAs("jwttoken"))
        .check (CurrentPageCheck.save)
        .check (CsrfCheck.save))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
  
  val loginAsDefendant=
    group("CMCDef_070_Login_As_Defendant") {
        exec (http ("Login_As_Defendant")
        .post(IdAMURL+"/register?redirect_uri=${redirectURI}&client_id=${clientId}&state=${state}&scope=&jwt=${jwttoken}")
        .formParam (csrfParameter, csrfTemplate)
        .formParam ("username", "${defuser}")
        .formParam ("password", "Pa55word11")
        .check(status.in(200,201,204))
        .check (regex (" Claims made against you"))
      )
        //.check (CurrentPageCheck.save)
        //.check (CsrfCheck.save))
    }
      .pause (MinThinkTime seconds, MaxThinkTime seconds)
    
}
