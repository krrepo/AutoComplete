# Ensure we require the local version and not one we might have installed already
require File.join([File.dirname(__FILE__),'lib','tasc','version.rb'])

spec = Gem::Specification.new do |s| 
  s.name = 'tasc'
  s.version = Tasc::VERSION
  s.author = 'Steve Quince'
  s.email = 'squince@glgroup.com'
  s.homepage = 'http://github.com/glg/AutoComplete'
  s.platform = Gem::Platform::RUBY
  s.summary = 'TrieService Auto-Suggest Command-Line Client for managing term lists'

  # Add other files here as they are created in the project
  
  s.files = Dir.glob("{bin,lib}/**/*") + %w(README.md)
  s.require_paths << 'lib'
  s.has_rdoc = false
  s.bindir = 'bin'
  s.executables << 'tasc'

  # List dependencies here
  s.add_dependency('rest-client')
  s.add_dependency('json')
  s.add_dependency('awesome_print')
  s.add_dependency('hirb')
  s.add_dependency('wopen3')
  s.add_dependency('win32console') if ($platform.to_s =~ /win32|mswin|mingw|cygwin/ || $platform.to_s == 'ruby')
  s.add_development_dependency('rake')
  s.add_development_dependency('rdoc')
  s.add_development_dependency('aruba')
  s.add_runtime_dependency('gli','2.5.4')
end
