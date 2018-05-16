'use strict';

var log = require('fancy-log');
var gulp = require('gulp');
var htmlreplace = require('gulp-html-replace');
var es = require('event-stream');
var concat = require('gulp-concat');
var ngAnnotate = require('gulp-ng-annotate');
var uglifyes = require('gulp-uglify-es').default;
var ngTemplateCache = require('gulp-angular-templatecache');
var autoprefixer = require('gulp-autoprefixer');
var csso = require('gulp-csso');
var replace = require('gulp-string-replace');

var randExtention = Math.floor(Math.random() * 10000000);
var confFile = './conf/local.js';


gulp.task('htmlreplace', function () {
    return gulp.src('src/index.html')
        .pipe(htmlreplace({
            'vendor': 'scripts/vendor-' + randExtention + '.js',
            'source': 'scripts/bundle-' + randExtention + '.js',
            'style': 'styles/app-' + randExtention + '.css'
        }))
        .pipe(gulp.dest('dist'));
});

gulp.task('configlocal', function () {
    confFile = './conf/local.js';
});

gulp.task('configtest', function () {
    confFile = './conf/test.js';
});

gulp.task('configprod', function () {
    confFile = './conf/prod.js';
});

gulp.task('bundle:source', function () {

    var config = require(confFile);
    return es.merge(
        gulp.src('src/app/**/*.html')
            .pipe(ngTemplateCache({
                module: 'NeLSApp',
                root: 'app'
            })),
        gulp.src([
            'src/app/route.js',
            'src/app/nels.js',
            'src/app/alert.js',
            'src/app/**/*.js'
        ])
    )
        .pipe(replace("_apiUrl_", config.tokens.apiUrl))
        .pipe(replace("_oauthUrl_", config.tokens.oauthUrl))
        .pipe(replace("_logoutUrl_", config.logout.url))
        .pipe(concat('bundle-' + randExtention + '.js'))
        .pipe(ngAnnotate())
        .pipe(uglifyes())
        .pipe(gulp.dest('dist/scripts'));
});

gulp.task('bundle:vendor', function () {
    var config = require(confFile);
    return gulp.src([
        'src/assets/libs/jquery.min.js',
        'src/assets/libs/jquery.jqplot.min.js',
        'src/assets/libs/jqplot.pieRenderer.js',
        'src/assets/libs/jqplot.donutRenderer.js',
        'src/assets/libs/angular.min.js',
        'src/assets/libs/angular-ui-router.min.js',
        'src/assets/libs/angular-local-storage.min.js',
        'src/assets/libs/angular-animate.min.js',
        'src/assets/libs/angular-touch.min.js',
        'src/assets/libs/loading-bar.min.js',
        'src/assets/libs/ui-bootstrap-tpls-1.3.2.min.js',
        'src/assets/libs/angular-sanitize.min.js',
        'src/assets/libs/select.js',
        'src/assets/libs/ui-chart.min.js',
        'src/assets/libs/matomo.js'
    ])
        .pipe(replace("_analyticsUrl_", config.analytics.url))
        .pipe(replace("_analyticsId_", config.analytics.id))
        .pipe(concat('vendor-' + randExtention + '.js'))
        .pipe(gulp.dest('dist/scripts'));
});

gulp.task('bundle:style', function () {
    return gulp.src([
        'src/assets/bootstrap/css/bootstrap.min.css',
        'src/assets/css/dashboard.css',
        'src/assets/css/loading-bar.css',
        'src/assets/css/nels.css',
        'src/assets/css/nels-sidebar.css',
        'src/assets/css/select.css',
        'src/assets/libs/jquery.jqplot.min.css'
    ])
        .pipe(concat('app-' + randExtention + '.css'))
        .pipe(autoprefixer())
        .pipe(csso())
        .pipe(gulp.dest('dist/styles'));
});

gulp.task('copy:assets', function () {
    gulp.src('src/assets/bootstrap/fonts/**/*')
        .pipe(gulp.dest('dist/fonts'));
    gulp.src('src/assets/images/**/*')
        .pipe(gulp.dest('dist/images'))
});

gulp.task('build-local', ['htmlreplace', 'configlocal', 'bundle:source', 'bundle:vendor', 'bundle:style', 'copy:assets']);
gulp.task('build-test', ['htmlreplace', 'configtest', 'bundle:source', 'bundle:vendor', 'bundle:style', 'copy:assets']);
gulp.task('build-prod', ['htmlreplace', 'configprod', 'bundle:source', 'bundle:vendor', 'bundle:style', 'copy:assets']);

