(ns crayon.examples.example01
  (:require
   [clojure.string :as str]
   [crayon.core :as c]))

(println (c/=> "Hello World!!"))
(println (c/=> :blue "Hello"))
(println (c/=> :black "Hello " "World" "!!"))
(println (c/=> :black "Hello " :yellow "World" :magenta "!!"))
(println (c/=> :bg-black :white "Hello World!!"))
(println (c/=> :bg-black :white "Hello " :white :bg-blue "World!!"))
(println (c/=> :strike :bold "Hello " :italic "World" :underline "!!"))
(println (c/=> (c/rgb 125 0 125) "Hello " :#00aaff "World!!"))

(println
 (str/join "," (for [i (range 255)]
                 (c/=> (c/rgb (rand-int 255) (rand-int 255) (rand-int 255)) (str i)))))

