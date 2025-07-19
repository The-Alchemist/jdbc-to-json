(ns jdbc-to-json.logging
  (:require [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn configure-logging!
  "Configure Timbre for colorized structured logging"
  []
  (log/merge-config!
    {:level :info
     :output-fn (fn [{:keys [level ?err msg_ ?ns-str ?file timestamp_ ?line vargs]}]
                  (let [timestamp (force timestamp_)
                        message (force msg_)
                        level-color (case level
                                      :trace  "\u001B[37mTRACE\u001B[0m"  ; white
                                      :debug  "\u001B[36mDEBUG\u001B[0m"  ; cyan
                                      :info   "\u001B[32mINFO \u001B[0m"  ; green
                                      :warn   "\u001B[33mWARN \u001B[0m"  ; yellow
                                      :error  "\u001B[31mERROR\u001B[0m"  ; red
                                      :fatal  "\u001B[35mFATAL\u001B[0m"  ; magenta
                                      (str level))
                        ns-short (when ?ns-str 
                                  (last (str/split ?ns-str #"\.")))]
                    (str "\u001B[90m" timestamp "\u001B[0m "  ; gray timestamp
                         level-color " "
                         "\u001B[34m[" ns-short "]\u001B[0m "  ; blue namespace
                         "\u001B[97m" message "\u001B[0m"      ; bright white message
                         (when ?err (str "\n" (log/stacktrace ?err))))))
     :appenders {:println (log/println-appender {:stream :auto})}})) 
