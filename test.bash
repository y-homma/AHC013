dt=`date +%Y%m%d%H%M%s`
for id in {0..99} ; do
      num=$(printf "%04d\n" "${id}")
      java Main.java ./in/${num}.txt
      read LINE < ./in/${num}.txt
      echo "$LINE" >> ./summary/${dt}.txt
      cargo run --release --bin vis ./in/${num}.txt ./out/${num}.txt >> ./summary/${dt}.txt
done