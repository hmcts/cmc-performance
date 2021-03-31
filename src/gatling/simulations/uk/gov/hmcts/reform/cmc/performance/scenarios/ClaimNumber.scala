
package uk.gov.hmcts.reform.cmc.performance.scenarios
import java.io.{BufferedWriter, FileWriter}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.Environment

object ClaimNumber {

val BaseURL = Environment.baseURL
val IdAMURL = Environment.idamAPIURL
  val idamRedirectURL="https://moneyclaims.perftest.platform.hmcts.net/receiver"
    val idamClient="cmc_citizen"
    val clientSecret="wrubu7ruprupAq6trexe3enay4memej7"
   
    
    val getIdamAuthCode =
        exec(http("TX010_EM_Bundle_IdamAuthCode")
          .post(IdAMURL + "/oauth2/authorize/?response_type=code&client_id=" + idamClient + "&redirect_uri=" + idamRedirectURL + "&scope=openid profile roles")
          .header("Content-Type", "application/x-www-form-urlencoded")
          .basicAuth("${defemail}","Pa55word11")
          .header("Content-Length", "0")
          .check(status.is(200))
          .check(jsonPath("$..code").optional.saveAs("serviceauthcode")))
          .pause(10)//original value is 50
          
          .doIf(session => session.contains("serviceauthcode")) {
            exec(http("TX020_EM_Bundle_Oauth2Token")
              .post(IdAMURL + "/oauth2/token?grant_type=authorization_code&code=" + "${serviceauthcode}" + "&client_id=" + idamClient + "&redirect_uri=" + idamRedirectURL + "&client_secret=" + clientSecret)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .header("Content-Length", "0")
              .check(jsonPath("$..access_token").optional.saveAs("accessToken"))
              .check(status.is(200)))
              .pause(5)// original value is 50
        }
  
  val getClaimNumber = exec(http("RD16_External_GetOrganizations")
    .get("http://cmc-claim-store-perftest.service.core-compute-perftest.internal/claims/pagination-metadata?userType=defendant")
    .header("Authorization", "Bearer ${accessToken}")
    .header("Content-Type", "application/json")
    .check(status is 200)
    .check(jsonPath("$..totalClaims").optional.saveAs("totalClaims"))
  )
      .pause(10)
  
    .exec {
      session =>
        val fw = new BufferedWriter (new FileWriter ("defendantclaimsnumber.csv", true))
        try {
          fw.write (session ("totalClaims").as [ String ] + ","  + session ("defemail").as [ String ] + "\r\n")
        }
        finally fw.close ()
        session
    }
  

      }
