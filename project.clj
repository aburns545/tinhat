(defproject tinhat "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [figwheel-sidecar "0.5.16"]
                 [cljsjs/react-bootstrap "0.31.0-0" :exclusions [cljsjs/react]]
                 [cljs-ajax "0.7.4"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [cljs-ajax "0.7.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3452
             }

  :profiles
  {:dev
         {:dependencies [[binaryage/devtools "0.9.10"]]

          :plugins      [[lein-figwheel "0.5.16"]]}
   :prod { }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "script" "src/clj"]
     :figwheel     {:on-jsload "tinhat.core/mount-root"}
     :compiler     {:main                 tinhat.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            tinhat.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}
  )
