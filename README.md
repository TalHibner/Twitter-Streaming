The Task: build an application providing keyword search over tweets (please use Java).

Breakdown:
-
1. Connect to the twitter stream via the Twitter API.
2. Save 100,000 tweets in English.
3. Build an index over all the tokens in the tweets to allow for single keyword searches.
4. Provide an interface which will allow the user to input a single word and return a list of all the tweets mentioning it in the Tweet text.

Extra:
-
The Task: extend the simple word search to provide synonym search as well.
Breakdown:
5. Use WordNet to find all the tweets that explicitly mention the keyword or a synonym. The synonyms are those words that are in the same synset in WordNet (see details below for an explanation).

Details:
-
In order to connect with the Twitter Public API you will first need to create an App under you Twitter account (go ahead and get one if you don't have it). Go to https://apps.twitter.com/ to create your application and get you OAuth credentials which you will use whenever you connect to the Twitter API. There are many tutorials explaining how to create a Twitter App and getting the necessary OAuth credentials.
Then you go over https://dev.twitter.com/streaming/overview to understand how the streaming api works. You can connect to the stream in whichever way you choose it doesn't matter to us. There are many tutorials on how to do it.
In order to identify the English tweets you can use the Tweet metadata language parameter which is included with every Tweet structure you will receive via the API.
Once you get the tweets you can save them however you'd like as long as it will be possible to index the text and retrieve them later via the user interface.
Use whitespace to tokenize (separate the different words) the tweet.
Use whatever you want to create the index.
Create any user interface you like - the only thing necessary is to be able to query with a single word and get back a result set containing all the tweets mentioning that word.

Extra Details:
-
WordNet (http://wordnet.princeton.edu/) groups words that have similar meaning into the same lexical set referred to as synset. So for every word, you need to look up all the synsets it is a member of and consider all the words in those synsets as synonyms. You can either download the WordNet database and use it locally or connect via an API whatever you want.
