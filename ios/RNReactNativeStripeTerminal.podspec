
Pod::Spec.new do |s|
  s.name         = "RNReactNativeStripeTerminal"
  s.version      = "1.0.0"
  s.summary      = "RNReactNativeStripeTerminal"
  s.description  = <<-DESC
                  RNReactNativeStripeTerminal
                   DESC
  s.homepage     = "https://github.com/BbotLLC/react-native-stripe-terminal"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/BbotLLC/react-native-stripe-terminal.git", :tag => "master" }
  s.source_files  = "RNReactNativeStripeTerminal/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

