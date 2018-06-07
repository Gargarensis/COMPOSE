var http = require("http");
var https = require("https");
var request = require('request');
var linkParse = require('parse-link-header');
var fs = require('fs');

let username = 'yourUser';
let password = 'yourPassword'

let url = 'https://api.github.com/';
let param = '?username=' + username + '&since=';

let javaRepos = JSON.parse(fs.readFileSync('./results/startResult.json', 'utf8'));


let minCommits = 500;

let javaCommits = [];

console.log(javaRepos.length);

let timeout = setTimeout(found, 10000);

function found() {
	console.log('More than 500 commits: ' + javaCommits.length);
	fs.writeFileSync('./results/commits.json', JSON.stringify(javaCommits));
}

for (let i = 0; i < javaRepos.length; i++) {
	let link = javaRepos[i].commits_url;
	link = link.substring(0, link.length - 6);
	getData(link, function(commits) {
		console.log(commits.length);
		if (commits.length > minCommits) {
			javaCommits.push(commits);
		}
		clearTimeout(timeout);
		timeout = setTimeout(found, 10000);
	});
}

function getData(uri, callback) {
	if (!uri) {
		return;
	}
	request({
		headers: {
			'User-Agent': username,
			'Host': 'api.github.com',
			'Authorization': 'Basic ' + new Buffer(username + ':' + password).toString('base64')
		},
		uri: uri,
		method: 'GET'
	}, function(err, res, body) {
		if (err) {
			console.log(err);
			return;
		}
		result = JSON.parse(body);
		if (callback) {
			callback(result);
		}
	});
}
