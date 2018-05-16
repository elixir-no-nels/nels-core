Introduction
===
This is the administration protal for NeLS

Initialize build environment
===
install package builders and tasks managers

### 3.1. install nodejs and npm
* for ubuntu:<br/>
sudo apt-get update<br/>
sudo apt-get install nodejs<br/>
sudo apt-get install npm
* for MAC OS:<br/>
brew install nodejs<br/>
brew install npm
###3.2. initialize project  & install build essentials
* cd nels-admin-portal folder
* npm init
* npm install --save  event-stream gulp gulp-html-replace gulp-concat gulp-ng-annotate gulp-uglify-es gulp-angular-templatecache gulp-autoprefixer gulp-csso gulp-string-replace

## 4. Run Build
* for local: rm -rf dist/ && gulp build-local
* for test: rm -rf dist/ && gulp build-test
* for prod: rm -rf dist/ && gulp build-prod <br/>
_**Note: you can optionally put these in .sh files for one go invocation**_

##5. Configure Webserver
###5.1. Configure proxy for NeLS API
* for apache 
        
        #nels api proxy
        SSLProxyEngine on
        ProxyPass /nels-api https://nels-root-url/nels-api
        ProxyPassReverse /nels-api https://nels-root-url/nels-api
* for nginx
        
        -
        
### 5.2. Host web application
 
 * for apache
        
        SSLProxyEngine on #since api is on https
        
        #NeLS admin web application
        Alias /nels-admin       /path-to-nels-admin-portal-code/dist
        <Directory "/path-to-nels-admin-portal-code/dist">
                #Require all granted
                Satisfy Any
                Allow from all
        </Directory>
 
 * for nginx

## 6. Access local site
* curl http://localhost/nels-admin
* use a modern browser and open the url: http://localhost/nels-admin
