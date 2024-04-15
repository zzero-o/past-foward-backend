#!/bin/bash
export SPRING_PROFILES_ACTIVE=prod

BUILD_JAR=$(ls /home/ec2-user/action/build/libs/*.jar)
JAR_NAME=$(basename "$BUILD_JAR")
echo "> build 파일명: $JAR_NAME" >> /home/ec2-user/action/deploy.log

echo "> 현재 실행중인 애플리케이션 PID 확인" >> /home/ec2-user/action/deploy.log
CURRENT_PID=$(pgrep -fl "$JAR_NAME" | grep -v grep | awk '{print $1}')

if [ -z "$CURRENT_PID" ]
then
  echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다." >> /home/ec2-user/action/deploy.log
else
  echo "> 애플리케이션 종료: PID $CURRENT_PID" >> /home/ec2-user/action/deploy.log
  kill -15 "$CURRENT_PID"
  while kill -0 "$CURRENT_PID" >/dev/null 2>&1; do
    echo "Waiting for process to terminate..." >> /home/ec2-user/action/deploy.log
    sleep 1
  done
  echo "> 애플리케이션이 종료되었습니다." >> /home/ec2-user/action/deploy.log
fi

echo "> build 파일을 $DEPLOY_PATH로 복사" >> /home/ec2-user/action/deploy.log
DEPLOY_PATH=/home/ec2-user/action/
cp "$BUILD_JAR" $DEPLOY_PATH

DEPLOY_JAR=$DEPLOY_PATH$JAR_NAME
echo "> $DEPLOY_JAR 배포" >> /home/ec2-user/action/deploy.log
nohup java -jar "$DEPLOY_JAR" > /home/ec2-user/deploy.log 2> /home/ec2-user/action/deploy_err.log &
