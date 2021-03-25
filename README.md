REPO 
- CMC Performance Testing

==================================================================

DESCRIPTION
- This repo consists of both the CMC Claimant and Defendant journeys

==================================================================

Claimant JOURNEY
- Citizen create claims against a Defendant


==================================================================

Defendant Response  JOURNEY
- Once claim is created against the Defendant, then Defendant can login and 
-respond to the claim 

==================================================================

CMC DATA GENERATION

- Complete the claims against around 100 defendants make sure each defendant will have atleast 100 claims 
- 
- 30%: 10 drafts - User is created. 10 x drafts are created to the SYA_120_DecidedIndependent transaction
- 30%: 15 drafts - User is created. 15 x drafts are created to the SYA_120_DecidedIndependent transaction

This means that when the test starts, some users will Login, see no drafts and do the E2E journey, where as other 
will see a number of drafts (3 - 15), edit one and continue the rest of the journey

==================================================================

MYA DATA GENERATION

- Requires TYA numbers therefore, need to have cases that are submitted already
- Note - for MYA to work, an email (@mailnator domain) must be registered. This script does that
