libs:
	pip install -r requirement.txt
	echo "sucessful"
server:
	python backend/main.py
frontend:
	python frontend-client/main.py
