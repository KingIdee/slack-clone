var express = require('express');
var Webtask = require('webtask-tools');
var bodyParser = require('body-parser');
const Chatkit = require('@pusher/chatkit-server');
const jwt = require('express-jwt');
const jwksRsa = require('jwks-rsa');

const chatkit = new Chatkit.default({
    instanceLocator: "CHATKIT_INSTANCE_LOCATOR",
    key: "CHATKIT_SECRET_KEY",
  });

var app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

const jwtCheck = jwt({
  secret: jwksRsa.expressJwtSecret({
    cache: true,
    rateLimit: true,
    jwksUri: `https://idee.auth0.com/.well-known/jwks.json`
  }),

  // Validate the audience and the issuer.
  aud: 'https://slack-clone/',
  issuer: `https://idee.auth0.com/`,
  algorithms: ['RS256']
});

// Get access token from ChatKit
app.post('/token', function (req, res) {
  const authData = chatkit.authenticate({
        userId: req.query.user_id
      });
      console.log(authData.body.access_token);
      res.status(authData.status)
         .send(authData.body);
});


// Create new user in ChatKit
app.post('/user', jwtCheck, (req, res) => {

  var imageURL = req.body.imageURL;

  chatkit.createUser({
  id: req.body.email,
  name: req.body.name
  }).then(r => {
    res.send(r);
  }).catch((err) => {
    res.send(err);
  });

});

// Fetch all users in ChatKit instance
app.get('/users', jwtCheck, (req, res) => {

  chatkit.getUsers()
  .then((r) => {
    res.status(200).send(r);
  }).catch((err) => {
    res.send(err);
  });

});

module.exports = Webtask.fromExpress(app);
