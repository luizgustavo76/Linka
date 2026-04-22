How to use federations

Linka's biggest advantage is the ease of creating your own server and the simple implementation of federations in just a few lines of code.

to use federations follow this steps:

1.1-import the api LinkaFederations

1.2-to download the api, run: pip install LinkaFederations

2-to use the api in your code:
import linka #import the api
linka.connect_json("route.json") #-- your routes json
linka.start()

the route json in the method LinkaFederations This file is used to translate your server routes, identify servers you don't want to communicate with, and specify the domain your server is running on.

how to make this file?
{
"domain":"your-domain.com",
"blacklist":{},
"routes":{
"create-post":"/new-post",
"send-a-message":"/send-message",
"sign-out":"/out"
}
}

the domain is your domain on the server is running

blacklist is servers you dont want communicate

and routes is a translator of your routes to universal routes

now, your server support federations!
server routes and how to use

this topic explain how to use the routes of the server in your own client
login

/register
method:post
json="username", "password", "email"

/login
method:post
json="username", "password"
friends

/inbox
methods:post
json="username"
what is? is your inbox of friends requests and news

/accept
methods:post
json="receiver", "remittee",
what is? accept a friend requests

/send-friend
methods:post
json="receiver", "remittee"
what is? is a route to send a friend request
Chat

/send
methods:post
json="receiver", "remittee", "message
what is? a route to send message Chat

/view
methods:post
json="receiver", "remittee"
what is? a route of view Chat
Post

/new
methods:post
json="username, text_post, datetime"
what is?a route of creation of Post

/view
methods:get
endpoint:id_of_post
what is? a route of view a post with post_id
Note: Successful requests return HTTP status codes 200 or 201. Failed requests return error codes such as 400, 401, 403, 404, or 500 