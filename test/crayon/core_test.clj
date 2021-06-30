(ns crayon.core-test
  (:require
   [clojure.test :refer [are deftest testing]]
   [crayon.core :as c]))

(def cm c/color-map)

(defn fg
  [c s]
  (format "\033[38;5;%dm%s" (c cm) s))

(defn bg
  [c s]
  (format "\033[48;5;%dm%s" (c cm) s))

(defn with-reset
  [s]
  (str s c/reset-code))

(defn fmt-reset
  [fmt & args]
  (str (format fmt args) c/reset-code))

(deftest crayon-test
  (testing "With no colors"
    (are [args expected] (= expected (apply c/=> args))
      [""] c/reset-code
      ["Hello"] (with-reset "Hello")
      ["Hello " "World" "!!"] (with-reset "Hello World!!")))

  (testing "With colors"
    (are [args expected] (= expected (apply c/=> args))
      [:red] (with-reset "")
      [:red ""] (with-reset (fg :red ""))
      [:blue "Hello"] (with-reset (fg :blue "Hello"))
      [:black "Hello " "World" "!!"]
      (with-reset (str (fg :black "Hello ")
                       (fg :black "World")
                       (fg :black "!!")))

      [:red :white ""] (with-reset "\033[38;5;7m")
      [:blue :white "Hello"] (with-reset "\033[38;5;7mHello")
      [:black "Hello " :yellow "World" :magenta "!!"]
      (with-reset (str (fg :black "Hello ")
                       (fg :yellow "World")
                       (fg :magenta "!!")))))

  (testing "With background colors"
    (are [args expected] (= expected (apply c/=> args))
      [:bg-red] (with-reset "")
      [:bg-red ""] (with-reset (bg :red ""))
      [:bg-blue "Hello"] (with-reset (bg :blue "Hello"))
      [:bg-black "Hello " "World" "!!"]
      (with-reset (str (bg :black "Hello ")
                       (bg :black "World")
                       (bg :black "!!")))))

  (testing "With attributes"
    (are [args expected] (= expected (apply c/=> args))
      [:bold] (with-reset "")
      [:bold ""] (with-reset "\033[1m")
      [:bold "Hello"] (with-reset "\033[1mHello")
      ["Hello " :underline "World!!"]
      (with-reset "Hello \033[4mWorld!!")

      [:italic "Hello " "World!!"]
      (with-reset "\033[3mHello \033[3mWorld!!")

      [:underline "Hello " :italic "World" :bold "!!"]
      (with-reset "\033[4mHello \033[4;3mWorld\033[4;3;1m!!")))

  (testing "Nested"
    (are [args expected] (= expected (apply c/=> args))
      ["Hello " (c/=> "World") "!!"]
      (with-reset (format "Hello World%s!!" c/reset-code))

      ["Hello " (c/=> (c/=> "World")) "!!"]
      (with-reset (format "Hello World%s%s!!" c/reset-code c/reset-code))

      [:black "Hello " (c/=> :red "World") "!!"]
      (with-reset (format "\033[38;5;0mHello \033[38;5;0m%s\033[38;5;0m!!"
                          (with-reset "\033[38;5;1mWorld")))

      [:blue "Hello " (c/=> :black (c/=> :yellow "World") "!!")]
      (with-reset (str (fg :blue "Hello ")
                       (fg :blue (str
                                  (fg :black (fg :yellow "World"))
                                  c/reset-code
                                  (fg :black "!!")
                                  c/reset-code))))))

  (testing "Reset"
    (are [args expected] (= expected (apply c/=> args))
      [:red "Hello " :fg-reset "World"] (with-reset (str (fg :red "Hello ")
                                                         c/fg-reset-code
                                                         "World"))
      [:bg-red "Hello " :bg-reset "World"] (with-reset (str (bg :red "Hello ")
                                                            c/bg-reset-code
                                                            "World"))
      [:red "Hello " :bg-blue "World" :reset "!!"]
      (with-reset (str (fg :red "Hello ")
                       (fg :red (bg :blue "World"))
                       c/fg-reset-code
                       c/bg-reset-code
                       "!!"))

      [:red "Hello " :reset "World" :blue :bg-white "!!"]
      (with-reset (str (fg :red "Hello ")
                       c/fg-reset-code
                       c/bg-reset-code
                       "World"
                       (fg :blue (bg :white "!!"))))

      [:red "Hello " :bg-reset "World"] (with-reset (str (fg :red "Hello ")
                                                         "\033[38;5;1m"
                                                         c/bg-reset-code
                                                         "World"))
      [:bg-red "Hello " :fg-reset "World"] (with-reset (str (bg :red "Hello ")
                                                            c/fg-reset-code
                                                            (bg :red "World")))))

  (testing "RGB"
    (are [args expected] (= (str expected c/reset-code) (apply c/=> args))
      [(c/rgb 0 0 0) "Hello"] "\033[38;2;0;0;0mHello"
      [(c/rgb 0 0 0) "Hello " (c/rgb 255 255 255) "World"]
      (str "\033[38;2;0;0;0mHello \033[38;2;255;255;255mWorld")

      [(c/bg-rgb 255 255 255) (c/rgb 0 255 255) "Hello " (c/rgb 255 0 255) "World"]
      (str "\033[48;2;255;255;255m\033[38;2;0;255;255mHello \033[38;2;255;0;255mWorld")))

  (testing "Hex"
    (are [args expected] (= (str expected c/reset-code) (apply c/=> args))
      [(c/hex :#000000) "Hello"] "\033[38;2;0;0;0mHello"
      [:#000000 "Hello"] "\033[38;2;0;0;0mHello"
      [(c/hex :#000000) "Hello " (c/hex :#ffffff) "World"]
      (str "\033[38;2;0;0;0mHello \033[38;2;255;255;255mWorld")

      [:#000000 "Hello " :#ffffff "World"]
      (str "\033[38;2;0;0;0mHello \033[38;2;255;255;255mWorld")

      [(c/bg-hex :#ffffff) (c/hex :#00ffff) "Hello " (c/hex :#ff00ff) "World"]
      (str "\033[48;2;255;255;255m\033[38;2;0;255;255mHello \033[38;2;255;0;255mWorld")

      [(c/bg-hex :#ffffff) :#00ffff "Hello " :#ff00ff "World"]
      (str "\033[48;2;255;255;255m\033[38;2;0;255;255mHello \033[38;2;255;0;255mWorld"))))
