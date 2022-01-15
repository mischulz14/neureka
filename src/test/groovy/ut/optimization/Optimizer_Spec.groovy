package ut.optimization

import neureka.Neureka
import neureka.Tsr
import neureka.calculus.Function
import neureka.calculus.assembly.ParseUtil
import neureka.ndim.config.types.views.SimpleReshapeView
import neureka.view.TsrStringSettings
import spock.lang.Ignore

//import org.junit.Test
import spock.lang.Specification

class Optimizer_Spec extends Specification
{
    def setup() {
        Neureka.get().reset()
        // Configure printing of tensors to be more compact:
        Neureka.get().settings().view().tensors({ TsrStringSettings it ->
            it.isScientific      = true
            it.isMultiline       = false
            it.hasGradient       = true
            it.cellSize          = 1
            it.hasValue          = true
            it.hasRecursiveGraph = false
            it.hasDerivatives    = true
            it.hasShape          = true
            it.isCellBound       = false
            it.postfix           = ""
            it.prefix            = ""
            it.hasSlimNumbers    = false
        })
    }

    // WIP! : not yet completed! // FIXME: Not working in CI
    @Ignore
    def 'Dot product operation based weight feed forwarded calculation is being optimized'()
    {
        given :
            def data = Tsr.of([8, 4], [ // a-b*c
                                          1d,  2d,  2d, -3d,
                                          3d, -1d, -1d,  4d,
                                         -1d, -2d, -3d, -7d,
                                         -2d, -3d,  4d,  10d,
                                          4d,  5d, -1d,  9d,
                                          6d,  2d,  3d,  0d,
                                          7d,  3d,  2d,  1d,
                                         -4d, -4d,  2d,  4d

            ])
            def X = data[0..-1, 0..2].T()
        //def Y = data[0..-1, 3   ]
        expect:
            X.NDConf instanceof SimpleReshapeView
            X.NDConf.indicesMap()  == [ 8,1  ] as int[]
            X.NDConf.translation() == [ 1, 4 ] as int[]
            X.NDConf.offset()      == [ 0,0  ] as int[]
            X.NDConf.spread()      == [ 1,1  ] as int[]
            X.NDConf.shape()       == [ 3, 8 ] as int[]

        when:
            def w1 = Tsr.of([8, 3], ":-)").setRqsGradient(true)
            def w2 = Tsr.of([7, 8], "O.o").setRqsGradient(true)
            def w3 = Tsr.of([1, 7], ":P").setRqsGradient(true)

        then:
            w1.toString() == "(8x3):[-0.97380E0, -0.07925E0, 1.78121E0, -1.39653E0, -2.01835E0, -0.41131E0, 0.64522E0, -2.5104E0, 1.40632E0, -0.51236E0, -0.62507E0, -0.09238E0, -0.52655E0, 0.57702E0, -1.37303E0, -1.27665E0, 0.63007E0, -2.15027E0, -1.12862E0, 1.48548E0, -0.37397E0, -0.51683E0, 1.14686E0, 0.87993E0]:g:[null]"
            w2.toString() == "(7x8):[-0.24678E0, 0.81013E0, 0.23709E0, -0.63630E0, 0.96606E0, 1.48405E0, 0.02693E0, 0.21423E0, -1.29268E0, 0.62331E0, -2.10922E0, 0.11949E0, -1.68853E0, 0.61275E0, 0.24238E0, 0.18039E0, -1.06208E0, 2.36855E0, -0.17225E0, -0.01213E0, 0.80261E0, -1.2896E0, -0.00226E0, -0.04938E0, 0.76502E0, 0.21277E0, -0.09264E0, 0.86399E0, -0.27399E0, -0.56845E0, -0.40595E0, -0.89906E0, -0.32320E0, 0.54387E0, -0.26504E0, -0.43978E0, 1.58936E0, 1.00546E0, 0.03889E0, 0.16153E0, -0.27125E0, -0.63222E0, 3.87467E0, 0.31497E0, -0.47855E0, -0.01106E0, -1.39322E0, 0.23283E0, 0.36686E0, 0.25312E0, ... + 6 more]:g:[null]"
            w3.toString() == "(1x7):[-0.68675E0, 1.09107E0, 0.20312E0, -1.18979E0, 0.53206E0, 2.11926E0, 1.15821E0]:g:[null]"

        when:
            def f = Function.of("softplus(I[0])")
            //def abs = Function.create("abs(I[0])")
            //def dox = Function.create("I[0]xI[1]")
        then :
            f.toString() == "softplus(I[0])"

        when :
            Tsr s = w1.convDot(X)
            Tsr a = f(s)
            Tsr b = f(w2.convDot(a))
            def y = w3.convDot(b)
            //dox(new Tsr[]{abs(y-Y), Tsr.of(y.shape(), 1,)}) // TODO!

        then:
            //s.toString().contains("(8x1x8):[2.4301E0, -4.62335E0, -4.21131E0, 9.31021E0, -6.07271E0, -0.65769E0, -3.49196E0, 7.77466E0, -6.25586E0, -1.75993E0, 6.66718E0, 7.20285E0, -15.2666E0, -13.6498E0, -16.6534E0, 12.8369E0, -1.56292E0, 3.03975E0, 0.15659E0, 11.866E0, -11.3774E0, 3.06953E0, -0.20197E0, 10.2733E0, -1.94728E0, -0.81962E0, 2.03966E0, 2.53039E0, -5.08241E0, -4.60147E0, -5.64652E0, 4.36496E0, -2.11855E0, -0.78365E0, 3.49157E0, -6.17009E0, 2.15197E0, -6.12432E0, -4.70082E0, -2.94796E0, -4.31705E0, -2.30974E0, 6.46733E0, -7.93802E0, 0.19404E0, -12.8506E0, -11.3469E0, -1.71425E0, 1.09438E0, -4.49735E0, ... + 14 more]")
            //a.toString().contains("(8x1x8):[2.51447E0, 0.00977E0, 0.01471E0, 9.3103E0, 0.00230E0, 0.41742E0, 0.02998E0, 7.77508E0, 0.00191E0, 0.15876E0, 6.66845E0, 7.20359E0, 234.325E-9, 1.18019E-6, 58.549E-9, 12.8369E0, 0.19022E0, 3.08648E0, 0.77450E0, 11.866E0, 11.4511E-6, 3.11492E0, 0.59725E0, 10.2734E0, 0.13336E0, 0.36505E0, 2.16194E0, 2.60701E0, 0.00618E0, 0.00998E0, 0.00352E0, 4.37759E0, 0.11351E0, 0.37619E0, 3.52157E0, 0.00208E0, 2.26195E0, 0.00218E0, 0.00904E0, 0.05111E0, 0.01325E0, 0.09466E0, 6.46888E0, 356.849E-6, 0.79487E0, 2.62465E-6, 11.8064E-6, 0.16559E0, 1.38312E0, 0.01107E0, ... + 14 more]")
            //a.toString().contains("->d(8x1x8):[0.91909E0, 0.00972E0, 0.01461E0, 0.99990E0, 0.00229E0, 0.34125E0, 0.02954E0, 0.99957E0, 0.00191E0, 0.14679E0, 0.99872E0, 0.99925E0, 234.325E-9, 1.18019E-6, 58.549E-9, 0.99999E0, 0.17322E0, 0.95433E0, 0.53906E0, 0.99999E0, 11.4511E-6, 0.95561E0, 0.44967E0, 0.99996E0, 0.12485E0, 0.30584E0, 0.88489E0, 0.92624E0, 0.00616E0, 0.00993E0, 0.00351E0, 0.98744E0, 0.10730E0, 0.31353E0, 0.97044E0, 0.00208E0, 0.89585E0, 0.00218E0, 0.00900E0, 0.04983E0, 0.01316E0, 0.09031E0, 0.99844E0, 356.785E-6, 0.54836E0, 2.62464E-6, 11.8063E-6, 0.15261E0, 0.74920E0, 0.01101E0, ... + 14 more]")
            //b.toString().contains("(7x1x1x8):[0.83819E0, 1.41432E0, 17.2222E0, 5.00199E0, 4.07684E0, 1.3544E0, 0.98670E0, 8.51153E0, 0.05680E0, 958.493E-6, 1.22564E0, 12.6565E-15, 0.12510E0, 0.00116E0, 0.31582E0, 94.3094E-12, 0.05871E0, 0.69717E0, 10.102E0, 5.03684E0, 1.06336E0, 0.29335E0, 0.59144E0, 20.1326E0, 0.15525E0, 0.63708E0, 0.18598E0, 8.54193E0, 0.00689E0, 0.16093E0, 0.17911E0, 11.0132E0, 0.64571E0, 0.59928E0, 14.5845E0, 0.04185E0, 4.98678E0, 0.42310E0, 0.76093E0, 0.76263E0, 0.29862E0, 11.781E0, 0.05654E0, 40.0085E0, 0.00634E0, 12.4093E0, 2.75931E0, 30.8732E0, 0.01874E0, 2.07786E0, ... + 6 more]")
            //b.toString().contains("->d(7x1x1x8):[0.56751E0, 0.75691E0, 0.99999E0, 0.99327E0, 0.98303E0, 0.74189E0, 0.62719E0, 0.99979E0, 0.05522E0, 958.033E-6, 0.70643E0, 12.5761E-15, 0.11759E0, 0.00116E0, 0.27081E0, 94.3095E-12, 0.05702E0, 0.50201E0, 0.99995E0, 0.99350E0, 0.65470E0, 0.25424E0, 0.44647E0, 0.99999E0, 0.14379E0, 0.47116E0, 0.16971E0, 0.99980E0, 0.00687E0, 0.14865E0, 0.16399E0, 0.99998E0, 0.47571E0, 0.45079E0, 0.99999E0, 0.04098E0, 0.99317E0, 0.34498E0, 0.53276E0, 0.53356E0, 0.25816E0, 0.99999E0, 0.05497E0, 1.0, 0.00632E0, 0.99999E0, 0.93666E0, 0.99999E0, 0.01857E0, 0.87480E0, ... + 6 more]")
//
            //y.toString().contains(
            //        "(8):[0.31170E0, 26.1058E0, -0.76121E0, 82.0447E0, 0.22348E0, 26.5363E0, 6.02289E0, 57.1377E0]"
            //)
            ParseUtil.similarity(
                    y.toString(),
                    "(8):[0.31170E0, 26.1058E0, -0.76121E0, 82.0447E0, 0.22348E0, 26.5363E0, 6.02289E0, 57.1377E0]; ->d({ends=[I@e0530f4}), "
            ) > 0.8
            //y.toString().contains(
            //        "(6x3):[-0.72226E0, 8.09081E0, 0.53085E0, 13.9085E0, -0.76121E0, 92.1329E0, 73.4495E0, " +
            //                "93.7233E0, -3.23894E0, -0.31264E0, -1.49487E0, 82.8246E0, -0.19767E0, 21.0283E0, " +
            //                "-0.34664E0, -1.4782E0, -2.6672E0, 4.5934E0]"
            //)


    }


}
