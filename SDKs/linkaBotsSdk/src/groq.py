import time
import requests
from linkaBotsSdk import LinkaBotSdk

# 1. Initialize the Bot SDK
bot = LinkaBotSdk()

# Groq API configuration
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
GROQ_HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Bearer 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",
}

def ask_groq(user_message):
    """Helper function to send the message to the AI and return the response."""
    payload = {
        "model": "openai/gpt-oss-120b",
        "messages": [
            {
                "role": "user",
                "content": user_message,
            }
        ],
    }
    
    try:
        response = requests.post(GROQ_URL, headers=GROQ_HEADERS, json=payload, timeout=15)
        if response.status_code == 200:
            return response.json()["choices"][0]["message"]["content"]
        else:
            print(f"Groq API Error ({response.status_code}): {response.text}")
            return None
    except Exception as e:
        print(f"Connection error with Groq: {e}")
        return None

# Dictionary to keep track of the last processed message per friend (prevents repeating)
last_processed_messages = {}

print("Bot is running and listening for messages... Press Ctrl+C to stop.\n")

# 2. Correct and safe main loop
while True:
    try:
        # Fetch and clean the friend list
        friends_list = bot.view_friends()
        
        if not friends_list:
            print("No friends found. Retrying in 10 seconds...")
            time.sleep(10)
            continue

        for friend in friends_list:
            # Fetch the last interaction with this friend
            last_interaction = bot.get_last_interaction(friend)
            
            if not last_interaction:
                continue

            sender = last_interaction.get("sender")
            message_text = last_interaction.get("message")

            # SAFETY CHECK 1: If the last message was sent BY US, ignore it (stops talking to itself)
            if sender and sender.lower() == bot.name.lower():
                continue

            # SAFETY CHECK 2: If we already processed this exact message, skip it
            if last_processed_messages.get(friend) == message_text:
                continue

            print(f"\n[New Message] From {friend}: '{message_text}'")
            print("Generating response with Groq AI...")

            # Generate response with AI
            ai_response = ask_groq(message_text)

            if ai_response:
                bot.send_chat(ai_response, friend)
                # Mark this message as processed for this friend
                last_processed_messages[friend] = message_text
                print(f"Response successfully sent to {friend}!")
            else:
                print(f"Failed to generate/send response to {friend}.")

        # Wait 5 seconds before checking for new messages again (prevents server spamming)
        time.sleep(5)

    except KeyboardInterrupt:
        print("\nBot stopped by user.")
        break
    except Exception as e:
        print(f"Error in main loop: {e}")
        time.sleep(5)