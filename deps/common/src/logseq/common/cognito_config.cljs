(ns logseq.common.cognito-config
  "Shared Cognito configuration for frontend and CLI-safe consumers.")

(def COGNITO-CLIENT-ID
  "logseq")

(def CLI-COGNITO-CLIENT-ID
  "69cs1lgme7p8kbgld8n5kseii6")

(def OAUTH-DOMAIN
  "logseq-prod.auth.us-east-1.amazoncognito.com")

(def OAUTH-SCOPE "openid email profile offline_access")

(goog-define OIDC-BASE-URL "")

(def OAUTH-AUTHORIZE-URL
  (str OIDC-BASE-URL "authorize/"))

(def OAUTH-TOKEN-URL
  (str OIDC-BASE-URL "token/"))

(def OAUTH-REDIRECT-URI
  "logseq://auth-callback")
