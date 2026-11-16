(ns electron.oidc
  (:require ["crypto" :as crypto]
            ["electron" :refer [shell]]
            [electron.logger :as logger]
            [electron.utils :refer [send-to-renderer]]
            [logseq.common.cognito-config :as cognito-config]
            [promesa.core :as p]))

(defonce *pending (atom nil))

(defn- random-string
  []
  (.toString (.randomBytes crypto 32) "base64url"))

(defn- parse-json
  [text]
  (try
    (js->clj (js/JSON.parse text) :keywordize-keys true)
    (catch :default _
      nil)))

(defn- <post-token
  [params]
  (p/let [resp (js/fetch cognito-config/OAUTH-TOKEN-URL
                         #js {:method "POST"
                              :headers #js {"content-type" "application/x-www-form-urlencoded"}
                              :body (.toString (js/URLSearchParams. (clj->js params)))})
          text (.text resp)]
    {:status (.-status resp)
     :body (parse-json text)}))

(defn start-login!
  [^js win]
  (let [verifier (random-string)
        state (random-string)
        challenge (-> (.createHash crypto "sha256")
                      (.update verifier)
                      (.digest "base64url"))
        params (js/URLSearchParams.
                (clj->js {:response_type "code"
                          :client_id cognito-config/COGNITO-CLIENT-ID
                          :redirect_uri cognito-config/OAUTH-REDIRECT-URI
                          :scope cognito-config/OAUTH-SCOPE
                          :state state
                          :code_challenge challenge
                          :code_challenge_method "S256"}))]
    (reset! *pending {:verifier verifier :state state :win win})
    (.openExternal shell (str cognito-config/OAUTH-AUTHORIZE-URL "?" params))))

(defn <refresh
  [refresh-token]
  (<post-token {:grant_type "refresh_token"
                :client_id cognito-config/COGNITO-CLIENT-ID
                :refresh_token refresh-token}))

(defn- notify-error!
  [^js win message]
  (logger/error ::oidc message)
  (send-to-renderer win :notification {:type "error" :payload message}))

(defn handle-callback!
  [^js win ^js parsed-url]
  (let [params (.-searchParams parsed-url)
        code (.get params "code")
        {:keys [verifier state] pending-win :win} @*pending
        target (if (and pending-win (not (.isDestroyed pending-win))) pending-win win)]
    (reset! *pending nil)
    (if (or (nil? state) (not= state (.get params "state")) (empty? code))
      (notify-error! target (str "OIDC login failed: " (or (.get params "error") "invalid state")))
      (-> (p/let [{:keys [status body]} (<post-token {:grant_type "authorization_code"
                                                      :client_id cognito-config/COGNITO-CLIENT-ID
                                                      :redirect_uri cognito-config/OAUTH-REDIRECT-URI
                                                      :code code
                                                      :code_verifier verifier})]
            (if (and (= 200 status) (:id_token body) (:refresh_token body))
              (send-to-renderer target :loginCallback
                                {:idToken {:jwtToken (:id_token body)}
                                 :accessToken {:jwtToken (:access_token body)}
                                 :refreshToken {:token (:refresh_token body)}})
              (notify-error! target (str "OIDC token exchange failed: " status " " (:error body)))))
          (p/catch #(notify-error! target (str "OIDC token exchange failed: " (.-message %))))))))
