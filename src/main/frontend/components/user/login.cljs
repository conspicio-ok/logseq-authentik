(ns frontend.components.user.login
  (:require [clojure.string :as string]
            [dommy.core :refer-macros [sel]]
            [electron.ipc :as ipc]
            [frontend.context.i18n :refer [t]]
            [frontend.handler.route :as route-handler]
            [frontend.handler.user :as user]
            [frontend.modules.shortcut.core :as shortcut]
            [frontend.rfx :as rfx]
            [frontend.state :as state]
            [io.factorhouse.hsx.core :as hsx]
            [logseq.shui.hooks :as hooks]
            [logseq.shui.ui :as shui]))

(defn sign-out!
  []
  nil)

(hsx/defc page-impl
  []
  (let [refresh-token (rfx/use-sub [:auth/refresh-token])]
    (hooks/use-effect!
     (fn []
       (when (user/logged-in?)
         (shui/dialog-close!)
         (shui/popup-hide!)
         (when (= :user-login (state/get-current-route))
           (route-handler/redirect! {:to :home}))))
     [refresh-token])
    [:div.cp__user-login
     (shui/card-header
      {:class "px-0"
       :data-auth-title-key "login"}
      (shui/card-title
       {:class "capitalize"}
       (t :ui/login)))
     [:div.ls-authenticator-content
      [shui/button {:class "w-full" :on-click #(ipc/ipc :oidc/login)}
       (t :account/sign-in)]]]))

(hsx/defc dialog-inner
  []
  (shortcut/use-disable-all-shortcuts!)
  (page-impl))

(hsx/defc page
  []
  [:div.pt-10 (page-impl)])

(defn open-login-modal!
  []
  (shui/dialog-open!
   (fn [_close] (dialog-inner))
   {:label :user-login
    :content-props {:onPointerDownOutside #(if (seq (sel "[data-auth-title-key='login']"))
                                             (let [inputs (sel ".ls-authenticator-content form input:not([type=checkbox])")
                                                   inputs (some->> inputs (map (fn [^js e] (.-value e))) (remove string/blank?))]
                                               (when (seq inputs)
                                                 (.preventDefault %)))
                                             (.preventDefault %))}}))
